package com.maxrave.data.listentogether

import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.repository.ListenTogetherRepository
import com.maxrave.logger.Logger
import kotlin.math.abs
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.simpmusic.listentogether.ListenTogetherSession
import org.simpmusic.listentogether.PlaybackActions
import org.simpmusic.listentogether.TrackInfo

private const val PLAY_SETTLE_TIMEOUT_MS = 2_000L
private const val PLAY_SETTLE_POLL_MS = 50L
private const val TAG = "ListenTogetherBridge"

private data class RoomSnapshot(
    val track: RoomTrack?,
    val isPlaying: Boolean,
    val position: Long,
    val queueIds: List<String>,
)

class ListenTogetherPlaybackBridge(
    private val repository: ListenTogetherRepository,
    private val session: ListenTogetherSession,
    private val handler: MediaPlayerHandler,
    private val scope: CoroutineScope,
) {
    private var started = false
    private var applyingRemote = false
    private var lastPublishedTrackId: String? = null
    private var lastAppliedTrackId: String? = null
    private var lastAppliedQueueIds: List<String> = emptyList()
    private var lastRoomPlaying = false
    private var lastSeekTimestamp = 0L

    fun start() {
        if (started) return
        started = true
        Logger.i(TAG, "Playback bridge started")
        scope.launch { suppressCrossfadeWhileInRoom() }
        scope.launch { watchRoomForGuests() }
        scope.launch { resyncGuestOnResume() }
        scope.launch { publishCurrentStateOnJoin() }
        scope.launch { publishStateWhenSomeoneArrives() }
        scope.launch { publishQueueAsHost() }
        scope.launch { publishTrackChangesAsHost() }
        scope.launch { publishPlayPauseAsHost() }
        scope.launch { publishSeeksAsHost() }
        scope.launch { answerBufferBarrier() }
    }

    private suspend fun suppressCrossfadeWhileInRoom() {
        repository.room
            .map { it.inRoom }
            .distinctUntilChanged()
            .collect { inRoom ->
                withContext(Dispatchers.Main) { handler.player.crossfadeSuppressed = inRoom }
                if (!inRoom) {
                    lastRoomPlaying = false
                } else if (!repository.room.value.isHost) {
                    repository.requestSync()
                }
                Logger.i(TAG, if (inRoom) "Crossfade suppressed for the room" else "Crossfade restored")
            }
    }

    private suspend fun resyncGuestOnResume() {
        handler.controlState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { locallyPlaying ->
                val room = repository.room.value
                if (!room.inRoom || room.isHost || applyingRemote) return@collect
                if (!locallyPlaying) {
                    Logger.i(TAG, "Guest paused locally — leaving the room running")
                    return@collect
                }
                Logger.i(TAG, "Guest resumed — asking the server where the room is")
                repository.requestSync()
            }
    }

    // ─────────────────────────── guest: follow the host ───────────────────────────

    private suspend fun watchRoomForGuests() {
        repository.room
            .map {
                RoomSnapshot(
                    track = it.currentTrack,
                    isPlaying = it.isPlaying,
                    position = it.position,
                    queueIds = it.queue.map { t -> t.id },
                ) to (it.inRoom && !it.isHost)
            }
            .distinctUntilChanged()
            .collect { (snapshot, shouldFollow) ->
                if (!shouldFollow) return@collect
                val (track, isPlaying, position, queueIds) = snapshot
                Logger.i(TAG, "Room says: track=${track?.id} playing=$isPlaying pos=$position queue=${queueIds.size}")
                applyingRemote = true
                try {
                    val queueChanged = queueIds != lastAppliedQueueIds
                    val trackChanged =
                        track != null && track.id.isNotBlank() && (track.id != lastAppliedTrackId || queueChanged)
                    val playing = if (trackChanged && !isPlaying) lastRoomPlaying else isPlaying
                    lastRoomPlaying = playing
                    if (trackChanged && track != null) {
                        val sameTrack = track.id == lastAppliedTrackId
                        lastAppliedTrackId = track.id
                        lastAppliedQueueIds = queueIds

                        val startAt =
                            when {
                                sameTrack -> handler.player.currentPosition
                                position <= 0L -> 0L
                                else -> session.positionAt(position, playing)
                            }
                        playTrack(track, keepPosition = startAt, playWhenReady = playing)
                    }
                    applyTransport(playing, position)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to apply remote state: ${e.message}")
                } finally {
                    applyingRemote = false
                }
            }
    }

    private suspend fun applyTransport(
        isPlaying: Boolean,
        position: Long,
    ) = withContext(Dispatchers.Main) {
        val now = System.currentTimeMillis()
        // Settle cooldown: Baar baar seek loop banne se rokne ke liye 1200ms ka cooldown
        val inCooldown = (now - lastSeekTimestamp) < SEEK_COOLDOWN_MS

        val corrected = session.positionAt(position, isPlaying)
        val current = handler.player.currentPosition
        val drift = abs(current - corrected)

        // Balanced tolerance window: 400ms (no stutter, no loop)
        if (!inCooldown && drift > SEEK_TOLERANCE_MS) {
            Logger.i(TAG, "Syncing guest: current=$current, target=$corrected, diff=${drift}ms")
            lastSeekTimestamp = now
            handler.player.seekTo(corrected)
        }

        if (isPlaying && !handler.player.playWhenReady) {
            handler.player.play()
        } else if (!isPlaying && handler.player.playWhenReady) {
            handler.player.pause()
        }
    }

    private suspend fun playTrack(
        info: RoomTrack,
        keepPosition: Long = 0L,
        playWhenReady: Boolean,
    ) {
        val roomQueue = repository.room.value.queue
        val ordered =
            (listOf(info) + roomQueue.filter { it.id != info.id })
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
        Logger.i(TAG, "Guest loading ${info.id} (${info.title}) + ${ordered.size - 1} upcoming")

        withContext(Dispatchers.Main) {
            handler.clearMediaItems()
            handler.addMediaItem(ordered.first().toRoomMediaItem(), playWhenReady = playWhenReady)
            val rest = ordered.drop(1)
            if (rest.isNotEmpty()) handler.addMediaItemList(rest.map { it.toRoomMediaItem() })
            if (keepPosition > 0L) handler.player.seekTo(keepPosition)
            lastSeekTimestamp = System.currentTimeMillis()
        }
    }

    private fun RoomTrack.toRoomMediaItem(): GenericMediaItem =
        GenericMediaItem(
            mediaId = id,
            uri = id,
            metadata =
                GenericMediaMetadata(
                    title = title,
                    artist = artist.ifBlank { null },
                    albumTitle = album.ifBlank { null },
                    artworkUri = thumbnail.ifBlank { null },
                    description = MERGING_DATA_TYPE.SONG,
                ),
            customCacheKey = id,
        )

    // ─────────────────────────── host: publish what we do ───────────────────────────

    private suspend fun publishCurrentStateOnJoin() {
        repository.room
            .map { it.inRoom && it.isHost }
            .distinctUntilChanged()
            .collect { isHosting -> if (isHosting) publishSnapshot() }
    }

    private suspend fun publishStateWhenSomeoneArrives() {
        repository.room
            .map { it.members.size }
            .distinctUntilChanged()
            .collect { count ->
                val state = repository.room.value
                if (state.inRoom && state.isHost && count > 1) publishSnapshot()
            }
    }

    private suspend fun publishQueueAsHost() {
        handler.queueData
            .map { (it as? QueueData.Data)?.listTracks?.map { t -> t.videoId }.orEmpty() }
            .distinctUntilChanged()
            .collect { ids ->
                val state = repository.room.value
                if (!state.inRoom || !state.isHost || applyingRemote || ids.isEmpty()) return@collect
                publishQueue()
            }
    }

    private fun publishQueue() {
        val data = handler.queueData.value as? QueueData.Data ?: return
        val tracks = data.listTracks.map { it.toTrackInfo() }
        if (tracks.isEmpty()) return
        session.sendQueue(tracks, data.playlistName.orEmpty())
        Logger.i(TAG, "Published queue of ${tracks.size} track(s)")
    }

    private fun publishSnapshot() {
        val item = handler.nowPlaying.value ?: return
        if (item.mediaId.isBlank()) return
        lastPublishedTrackId = item.mediaId
        val data = handler.queueData.value as? QueueData.Data
        session.sendPlaybackAction(
            action = PlaybackActions.CHANGE_TRACK,
            trackId = item.mediaId,
            position = handler.player.currentPosition,
            trackInfo = item.toTrackInfo(),
            queue = data?.listTracks.orEmpty().map { it.toTrackInfo() },
            queueTitle = data?.playlistName.orEmpty(),
        )
        session.sendPlaybackAction(
            action = if (handler.player.playWhenReady) PlaybackActions.PLAY else PlaybackActions.PAUSE,
            trackId = "",
            position = handler.player.currentPosition,
            trackInfo = null,
        )
        Logger.i(TAG, "Published current state to the room: ${item.mediaId}")
    }

    private suspend fun publishTrackChangesAsHost() {
        handler.nowPlaying
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.mediaId == new.mediaId }
            .collect { item ->
                val state = repository.room.value
                if (!state.inRoom || !state.isHost || applyingRemote) return@collect
                if (item.mediaId == lastPublishedTrackId) return@collect
                lastPublishedTrackId = item.mediaId
                Logger.i(TAG, "Host publishing track change: ${item.mediaId}")
                val data = handler.queueData.value as? QueueData.Data
                session.sendPlaybackAction(
                    action = PlaybackActions.CHANGE_TRACK,
                    trackId = item.mediaId,
                    position = 0L,
                    trackInfo = item.toTrackInfo(),
                    queue = data?.listTracks.orEmpty().map { it.toTrackInfo() },
                    queueTitle = data?.playlistName.orEmpty(),
                )

                val started =
                    withTimeoutOrNull(PLAY_SETTLE_TIMEOUT_MS) {
                        while (!handler.player.playWhenReady && !handler.player.isPlaying) {
                            delay(PLAY_SETTLE_POLL_MS)
                        }
                    } != null
                if (started) {
                    session.sendPlaybackAction(
                        action = PlaybackActions.PLAY,
                        trackId = "",
                        position = handler.player.currentPosition,
                        trackInfo = null,
                    )
                }
            }
    }

    private suspend fun publishPlayPauseAsHost() {
        handler.controlState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { isPlaying ->
                val state = repository.room.value
                if (!state.inRoom || !state.isHost || applyingRemote) return@collect
                val intent = handler.player.playWhenReady
                if (isPlaying != intent) return@collect
                Logger.i(TAG, "Host publishing ${if (intent) "PLAY" else "PAUSE"}")
                session.sendPlaybackAction(
                    action = if (intent) PlaybackActions.PLAY else PlaybackActions.PAUSE,
                    trackId = "",
                    position = handler.player.currentPosition,
                    trackInfo = null,
                )
            }
    }

    private suspend fun publishSeeksAsHost() {
        var lastProgress = 0L
        var lastAt = 0L
        handler.simpleMediaState.collect { mediaState ->
            val progress = (mediaState as? SimpleMediaState.Progress)?.progress ?: return@collect
            val now = PROCESS_START.elapsedNow().inWholeMilliseconds
            val previous = lastProgress
            val previousAt = lastAt
            lastProgress = progress
            lastAt = now

            val state = repository.room.value
            if (!state.inRoom || !state.isHost || applyingRemote) return@collect
            if (previousAt == 0L) return@collect

            val elapsed = now - previousAt
            val expected = previous + if (handler.player.isPlaying) elapsed else 0L
            if (abs(progress - expected) < SEEK_DETECT_MS) return@collect

            Logger.i(TAG, "Host publishing SEEK to $progress (expected ~$expected)")
            session.sendPlaybackAction(
                action = PlaybackActions.SEEK,
                trackId = "",
                position = progress,
                trackInfo = null,
            )
        }
    }

    // ─────────────────────────── the buffer barrier ───────────────────────────

    private suspend fun answerBufferBarrier() {
        repository.room
            .map { it.waitingFor to it.currentTrack?.id }
            .distinctUntilChanged()
            .collect { (waitingFor, trackId) ->
                val state = repository.room.value
                if (trackId.isNullOrBlank() || !state.inRoom) return@collect
                if (state.selfUserId !in waitingFor) return@collect

                if (handler.player.bufferedPercentage >= READY_BUFFER_PERCENT) {
                    session.reportBufferReady(trackId)
                }
            }
    }

    private fun Track.toTrackInfo(): TrackInfo =
        TrackInfo(
            id = videoId,
            title = title,
            artist = artists?.joinToString(", ") { it.name }.orEmpty(),
            album = album?.name.orEmpty(),
            duration = (durationSeconds?.toLong() ?: 0L) * 1000L,
            thumbnail = thumbnails?.lastOrNull()?.url.orEmpty(),
        )

    private fun GenericMediaItem.toTrackInfo(): TrackInfo =
        TrackInfo(
            id = mediaId,
            title = metadata.title.orEmpty(),
            artist = metadata.artist.orEmpty(),
            album = metadata.albumTitle.orEmpty(),
            duration = handler.player.duration.coerceAtLeast(0L),
            thumbnail = metadata.artworkUri.orEmpty(),
        )

    private companion object {
        val PROCESS_START = TimeSource.Monotonic.markNow()

        /** Balanced tolerance: ~400ms prevents stuttering loop while keeping audio in close sync */
        const val SEEK_TOLERANCE_MS = 400L

        /** Cooldown window to prevent repeating seek loops */
        const val SEEK_COOLDOWN_MS = 1_200L

        const val SEEK_DETECT_MS = 2_000L
        const val READY_BUFFER_PERCENT = 5
    }
}