package com.maxrave.data.mediaservice

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.ContentUris
import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.provider.MediaStore
import com.maxrave.common.ASC
import com.maxrave.common.CUSTOM_ORDER
import com.maxrave.common.Config.ALBUM_CLICK
import com.maxrave.common.Config.PLAYLIST_CLICK
import com.maxrave.common.Config.RADIO_CLICK
import com.maxrave.common.Config.RECOVER_TRACK_QUEUE
import com.maxrave.common.Config.SHARE
import com.maxrave.common.Config.SONG_CLICK
import com.maxrave.common.Config.VIDEO_CLICK
import com.maxrave.common.DESC
import com.maxrave.common.LOCAL_PLAYLIST_ID
import com.maxrave.common.LOCAL_PLAYLIST_ID_SAVED_QUEUE
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.common.SPONSOR_BLOCK_MIN_SEGMENT_SECONDS
import com.maxrave.common.SPONSOR_BLOCK_SKIP_MARGIN_MS
import com.maxrave.common.TITLE
import com.maxrave.data.db.Converters
import com.maxrave.data.lastfm.LastfmScrobbler
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.mediaService.SponsorSkipSegments
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.data.player.AudioEffects
import com.maxrave.domain.data.player.DelayEffect
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.data.player.GenericCommandButton
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.data.player.GenericPlaybackParameters
import com.maxrave.domain.data.player.GenericTracks
import com.maxrave.domain.data.player.PlayerConstants
import com.maxrave.domain.data.player.PlayerError
import com.maxrave.domain.data.player.ReverbEffect
import com.maxrave.domain.data.player.ReverbPreset
import com.maxrave.domain.extension.isVideo
import com.maxrave.domain.extension.now
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.extension.toSongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.FALSE
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.NowPlayingTrackState
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.mediaservice.handler.SleepTimerState
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.domain.mediaservice.player.MediaPlayerInterface
import com.maxrave.domain.mediaservice.player.MediaPlayerListener
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.MusicVideoType
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toArrayListTrack
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.my.kizzy.DiscordRPC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform.getKoin
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

private val TAG = "Media3ServiceHandlerImpl"

internal class MediaServiceHandlerImpl(
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val streamRepository: StreamRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val coroutineScope: CoroutineScope,
) : MediaPlayerHandler,
    MediaPlayerListener {
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val context: Context = getKoin().get()
    override val player: MediaPlayerInterface = getKoin().get()

    @Volatile
    private var discordRPC: DiscordRPC? = null

    private val lastfmScrobbler = LastfmScrobbler(dataStoreManager)
    override var onUpdateNotification: (List<GenericCommandButton>) -> Unit = {}
    override var showToast: (ToastType) -> Unit = {}
    override var pushPlayerError: (PlayerError) -> Unit = {}
    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    override val simpleMediaState: StateFlow<SimpleMediaState> = _simpleMediaState.asStateFlow()

    private val _nowPlaying = MutableStateFlow<GenericMediaItem?>(player.currentMediaItem)
    override val nowPlaying: StateFlow<GenericMediaItem?> = _nowPlaying.asStateFlow()

    private val _queueData =
        MutableStateFlow<QueueData>(
            QueueData(
                queueState = QueueData.StateSource.STATE_CREATED,
                data = QueueData.Data(),
            ),
        )
    override val queueData = _queueData.asStateFlow()

    private val _controlState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = player.isPlaying,
                isShuffle = player.shuffleModeEnabled,
                repeatState =
                    when (player.repeatMode) {
                        PlayerConstants.REPEAT_MODE_ONE -> {
                            RepeatState.One
                        }

                        PlayerConstants.REPEAT_MODE_ALL -> {
                            RepeatState.All
                        }

                        PlayerConstants.REPEAT_MODE_OFF -> {
                            RepeatState.None
                        }

                        else -> {
                            RepeatState.None
                        }
                    },
                isLiked = false,
                isNextAvailable = player.hasNextMediaItem(),
                isPreviousAvailable = player.hasPreviousMediaItem(),
                isCrossfading = false,
                volume = 1f,
            ),
        )

    override val controlState: StateFlow<ControlState> = _controlState.asStateFlow()

    private val _nowPlayingState = MutableStateFlow<NowPlayingTrackState>(NowPlayingTrackState.initial())
    override val nowPlayingState: StateFlow<NowPlayingTrackState> = _nowPlayingState.asStateFlow()

    private val _sleepTimerState = MutableStateFlow<SleepTimerState>(SleepTimerState(false, 0))
    override val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private val _skipSegments: MutableStateFlow<List<SponsorSkipSegments>?> = MutableStateFlow<List<SponsorSkipSegments>?>(null)
    override val skipSegments: StateFlow<List<SponsorSkipSegments>?> = _skipSegments.asStateFlow()

    private val _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow<NewFormatEntity?>(null)
    override val format: StateFlow<NewFormatEntity?> = _format.asStateFlow()

    private val _currentSongIndex: MutableStateFlow<Int> = MutableStateFlow(player.currentMediaItemIndex)
    override val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _castState = MutableStateFlow(GenericCastState.NOT_CASTING)
    override val castState: StateFlow<GenericCastState> = _castState.asStateFlow()

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var secondLoudnessEnhancer: LoudnessEnhancer? = null

    private var skipSilent = false
    private var normalizeVolume = false
    private var watchTimeList: ArrayList<Float> = arrayListOf()
    private var volumeNormalizationJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val sleepFadeDurationMs = 5_000L
    private val sleepFadeSteps = 50
    private val sleepFadeTailMs = 800L
    private var getSkipSegmentsJob: Job? = null
    private var getFormatJob: Job? = null
    private var progressJob: Job? = null
    private var bufferedJob: Job? = null
    private var updateNotificationJob: Job? = null
    private var toggleLikeJob: Job? = null
    private var loadJob: Job? = null
    private var songEntityJob: Job? = null
    private var jobWatchtime: Job? = null
    private var getDataOfNowPlayingTrackStateJob: Job? = null

    private val rpcEventSeq = AtomicLong(0L)

    private data class RpcSnapshot(
        val song: SongEntity,
        val progressMs: Long,
        val durationMs: Long,
        val speed: Float,
        val seq: Long,
    )

    private val rpcSnapshotFlow = MutableStateFlow<RpcSnapshot?>(null)

    @Volatile
    private var rpcSenderJob: Job? = null

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    private fun fromListIntToString(list: List<Int>?): String? = list?.let { json.encodeToString(list) }

    private fun fromStringToListInt(value: String?): List<Int>? =
        try {
            value?.let { json.decodeFromString<List<Int>>(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    init {
        player.addListener(this)
        progressJob = Job()
        bufferedJob = Job()
        sleepTimerJob = Job()
        volumeNormalizationJob = Job()
        updateNotificationJob = Job()
        toggleLikeJob = Job()
        loadJob = Job()
        songEntityJob = Job()
        getSkipSegmentsJob = Job()
        getFormatJob = Job()
        jobWatchtime = Job()
        skipSilent = runBlocking { dataStoreManager.skipSilent.first() == TRUE }

        backgroundScope.launch {
            combine(
                dataStoreManager.equalizerEnabled,
                dataStoreManager.equalizerBands,
                dataStoreManager.equalizerPreamp,
            ) { enabled, bands, preamp -> Triple(enabled == TRUE, bands, preamp) }
                .distinctUntilChanged()
                .collect { (enabled, bands, preamp) ->
                    player.setEqualizer(
                        bandsDb =
                            if (enabled) bands.split(",").mapNotNull { it.trim().toFloatOrNull() } else emptyList(),
                        preampDb = if (enabled) preamp else 0f,
                    )
                }
        }

        backgroundScope.launch {
            val delayEffects =
                combine(
                    dataStoreManager.delayEnabled,
                    dataStoreManager.delayTimeMs,
                    dataStoreManager.delayFeedback,
                    dataStoreManager.delayMix,
                ) { enabled, timeMs, feedback, mix ->
                    if (enabled == TRUE) DelayEffect(timeMs = timeMs, feedback = feedback, mix = mix) else null
                }
            val reverbEffects =
                combine(
                    dataStoreManager.reverbEnabled,
                    dataStoreManager.reverbPreset,
                    dataStoreManager.reverbMix,
                ) { enabled, presetName, mix ->
                    if (enabled == TRUE) {
                        ReverbEffect(
                            preset = runCatching { ReverbPreset.valueOf(presetName) }.getOrDefault(ReverbPreset.HALL),
                            mix = mix,
                        )
                    } else {
                        null
                    }
                }
            combine(delayEffects, reverbEffects) { echo, room -> AudioEffects(delay = echo, reverb = room) }
                .distinctUntilChanged()
                .collect { effects -> player.setAudioEffects(effects) }
        }

        normalizeVolume =
            runBlocking { dataStoreManager.normalizeVolume.first() == TRUE }
        _nowPlaying.value = player.currentMediaItem
        if (runBlocking { dataStoreManager.saveStateOfPlayback.first() } == TRUE) {
            val shuffleKey = runBlocking { dataStoreManager.shuffleKey.first() }
            val repeatKey = runBlocking { dataStoreManager.repeatKey.first() }
            val restoredShuffle = shuffleKey == TRUE
            val restoredRepeatMode =
                when (repeatKey) {
                    DataStoreManager.REPEAT_ONE -> PlayerConstants.REPEAT_MODE_ONE
                    DataStoreManager.REPEAT_ALL -> PlayerConstants.REPEAT_MODE_ALL
                    else -> PlayerConstants.REPEAT_MODE_OFF
                }
            player.shuffleModeEnabled = restoredShuffle
            player.repeatMode = restoredRepeatMode
            _controlState.value =
                _controlState.value.copy(
                    isShuffle = restoredShuffle,
                    repeatState =
                        when (restoredRepeatMode) {
                            PlayerConstants.REPEAT_MODE_ONE -> RepeatState.One
                            PlayerConstants.REPEAT_MODE_ALL -> RepeatState.All
                            else -> RepeatState.None
                        },
                )
        }
        mayBeRestoreQueue()
        coroutineScope.launch {
            val controlStateJob =
                launch {
                    controlState.collectLatest {
                        updateNotification()
                    }
                }
            val skipSegmentsJob =
                launch {
                    simpleMediaState
                        .filter { it is SimpleMediaState.Progress }
                        .map {
                            val current = (it as SimpleMediaState.Progress).progress
                            val duration = player.duration
                            if (duration > 0L) {
                                (current.toFloat() / player.duration) * 100
                            } else {
                                -1f
                            }
                        }.filter { it >= 0f }
                        .distinctUntilChanged()
                        .collect { current ->
                            if (dataStoreManager.sponsorBlockEnabled.first() == TRUE) {
                                if (player.duration > 0L) {
                                    val skipSegments = skipSegments.value
                                    val listCategory = dataStoreManager.getSponsorBlockCategories()
                                    if (skipSegments != null) {
                                        for (skip in skipSegments) {
                                            if (listCategory.contains(skip.category)) {
                                                if (skip.segment[1] - skip.segment[0] < SPONSOR_BLOCK_MIN_SEGMENT_SECONDS) {
                                                    continue
                                                }
                                                val firstPart = ((skip.segment[0] / skip.videoDuration) * 100).toFloat()
                                                val secondPart =
                                                    ((skip.segment[1] / skip.videoDuration) * 100).toFloat()
                                                if (current in firstPart..secondPart) {
                                                    skipSegment(
                                                        (secondPart * player.duration).toLong() / 100 + SPONSOR_BLOCK_SKIP_MARGIN_MS,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            val playbackJob =
                launch {
                    format.collectLatest { formatTemp ->
                        if (dataStoreManager.sendBackToGoogle.first() == TRUE) {
                            if (formatTemp != null) {
                                initPlayback(
                                    formatTemp.playbackTrackingVideostatsPlaybackUrl,
                                    formatTemp.playbackTrackingAtrUrl,
                                    formatTemp.playbackTrackingVideostatsWatchtimeUrl,
                                    formatTemp.cpn,
                                )
                            }
                        }
                    }
                }
            val playbackSpeedPitchJob =
                launch {
                    combine(dataStoreManager.playbackSpeed, dataStoreManager.pitch) { speed, pitch ->
                        Pair(speed, pitch)
                    }.collectLatest { pair ->
                        player.playbackParameters =
                            GenericPlaybackParameters(
                                pair.first,
                                2f.pow(pair.second.toFloat() / 12),
                            )
                        if (player.isPlaying) {
                            nowPlayingState.value.songEntity?.let { updateDiscordRpc(it) }
                        }
                    }
                }
            val discordRPCEnabledJob =
                launch {
                    combine(
                        dataStoreManager.richPresenceEnabled,
                        dataStoreManager.discordToken,
                    ) { enabled, token ->
                        enabled == TRUE && token.isNotBlank()
                    }.distinctUntilChanged().collectLatest { shouldRun ->
                        if (shouldRun) {
                            if (discordRPC == null) {
                                discordRPC = DiscordRPC(dataStoreManager.discordToken.first())
                            }
                            if (rpcSenderJob?.isActive != true) {
                                rpcSenderJob =
                                    coroutineScope.launch(Dispatchers.IO) {
                                        var lastHandledSeq = 0L
                                        rpcSnapshotFlow.filterNotNull().collectLatest { snap ->
                                            if (snap.seq < lastHandledSeq) return@collectLatest
                                            lastHandledSeq = snap.seq
                                            if (!controlState.value.isPlaying) return@collectLatest
                                            discordRPC
                                                ?.updateSong(snap.progressMs, snap.durationMs, snap.speed, snap.song)
                                                ?.onFailure { Logger.e(TAG, "Discord RPC update failed: ${it.message}") }
                                        }
                                    }
                                nowPlayingState.value.songEntity?.let { song ->
                                    backgroundScope.launch {
                                        updateDiscordRpc(song)
                                    }
                                }
                            }
                        } else {
                            withContext(NonCancellable) {
                                rpcSenderJob?.cancel()
                                rpcSenderJob = null
                                if (discordRPC?.isRpcRunning() == true) {
                                    discordRPC?.closeRPC()
                                }
                                discordRPC = null
                                rpcSnapshotFlow.value = null
                            }
                        }
                    }
                }
            controlStateJob.join()
            skipSegmentsJob.join()
            playbackJob.join()
            playbackSpeedPitchJob.join()
            discordRPCEnabledJob.join()
        }
    }

    private fun getDataOfNowPlayingState(mediaItem: GenericMediaItem) {
        val videoId =
            if (mediaItem.isVideo()) {
                mediaItem.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
            } else {
                mediaItem.mediaId
            }
        val track =
            queueData.value.data.listTracks
                ?.find { it.videoId == videoId }
        _nowPlayingState.update {
            it.copy(
                mediaItem = mediaItem,
                track = track,
            )
        }
        _format.value = null
        _skipSegments.value = null
        getDataOfNowPlayingTrackStateJob?.cancel()
        getDataOfNowPlayingTrackStateJob =
            coroutineScope.launch {
                songRepository.getSongById(videoId).cancellable().singleOrNull().let { songEntity ->
                    if (songEntity != null) {
                        _controlState.update { it.copy(isLiked = songEntity.liked) }
                        var thumbUrl =
                            track?.thumbnails?.lastOrNull()?.url
                                ?: songEntity.thumbnails
                                ?: "http://i.ytimg.com/vi/${songEntity.videoId}/maxresdefault.jpg"
                        thumbUrl = Regex("=w\\d+-h\\d+").replace(thumbUrl, "=w544-h544")
                        if (songEntity.thumbnails != thumbUrl) {
                            songRepository.updateThumbnailsSongEntity(thumbUrl, songEntity.videoId).singleOrNull()
                        }
                        MusicVideoType.normalize(track?.videoType)?.let { freshVideoType ->
                            if (songEntity.videoType != freshVideoType) {
                                songRepository.updateVideoTypeSongEntity(freshVideoType, songEntity.videoId).singleOrNull()
                            }
                        }
                        songRepository.updateSongInLibrary(now(), songEntity.videoId).singleOrNull()
                        songRepository.updateListenCount(songEntity.videoId)
                        _nowPlayingState.update {
                            it.copy(
                                songEntity =
                                    songEntity.copy(
                                        thumbnails = thumbUrl,
                                    ),
                            )
                        }
                        updateDiscordRpc(songEntity)
                        coroutineScope.launch { lastfmScrobbler.onTrackStarted(songEntity) }
                    } else {
                        _controlState.update { it.copy(isLiked = false) }
                        var thumbUrl =
                            track?.thumbnails?.lastOrNull()?.url
                                ?: "http://i.ytimg.com/vi/${track?.videoId}/maxresdefault.jpg"
                        thumbUrl = Regex("=w\\d+-h\\d+").replace(thumbUrl, "=w544-h544")
                        val songEntity =
                            (track?.toSongEntity() ?: mediaItem.toSongEntity()).copy(
                                thumbnails = thumbUrl,
                            )
                        songRepository.insertSong(songEntity).singleOrNull()
                        _nowPlayingState.update {
                            it.copy(
                                songEntity = songEntity,
                            )
                        }
                        updateDiscordRpc(songEntity)
                        coroutineScope.launch { lastfmScrobbler.onTrackStarted(songEntity) }
                    }
                }
                songEntityJob?.cancel()
                songEntityJob =
                    coroutineScope.launch {
                        songRepository.getSongAsFlow(videoId).cancellable().filterNotNull().collectLatest { songEntity ->
                            if (dataStoreManager.explicitContentEnabled.first() == FALSE && songEntity.isExplicit) {
                                showToast(ToastType.ExplicitContent)
                                if (player.hasNextMediaItem()) {
                                    player.seekToNext()
                                } else if (player.hasPreviousMediaItem()) {
                                    player.seekToPrevious()
                                } else {
                                    player.stop()
                                }
                                return@collectLatest
                            }
                            _nowPlayingState.update {
                                it.copy(
                                    songEntity = songEntity,
                                )
                            }
                            _controlState.update {
                                it.copy(
                                    isLiked = songEntity.liked,
                                )
                            }
                        }
                    }
                if (dataStoreManager.sponsorBlockEnabled.first() == TRUE) {
                    getSkipSegments(videoId)
                }
                if (dataStoreManager.sendBackToGoogle.first() == TRUE) {
                    getFormat(videoId)
                }
            }
    }

    private fun getSkipSegments(videoId: String) {
        _skipSegments.value = null
        coroutineScope.launch {
            streamRepository.getSkipSegments(videoId).collect { response ->
                when (response) {
                    is Resource.Success -> {
                        _skipSegments.value = response.data
                    }
                    is Resource.Error -> {
                        _skipSegments.value = null
                    }
                }
            }
        }
    }

    private fun getFormat(mediaId: String?) {
        getFormatJob?.cancel()
        getFormatJob =
            coroutineScope.launch {
                if (mediaId != null) {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                    }
                }
            }
    }

    private fun initPlayback(
        playback: String?,
        atr: String?,
        watchTime: String?,
        cpn: String?,
    ) {
        jobWatchtime?.cancel()
        coroutineScope.launch {
            if (playback != null && atr != null && watchTime != null && cpn != null) {
                watchTimeList = arrayListOf()
                streamRepository
                    .initPlayback(playback, atr, watchTime, cpn, queueData.value.data.playlistId)
                    .collect {
                        if (it.first == 204) {
                            watchTimeList.add(0f)
                            watchTimeList.add(5.54f)
                            watchTimeList.add(it.second)
                            updateWatchTime()
                        }
                    }
            }
        }
    }

    private fun updateWatchTime() {
        coroutineScope.launch {
            jobWatchtime =
                launch {
                    simpleMediaState.collect { state ->
                        if (state is SimpleMediaState.Progress) {
                            val value = state.progress
                            if (value > 0 && watchTimeList.isNotEmpty()) {
                                val second = (value / 1000).toFloat()
                                if (second in watchTimeList.last()..watchTimeList.last() + 1.2f) {
                                    val watchTimeUrl =
                                        _format.value?.playbackTrackingVideostatsWatchtimeUrl
                                    val cpn = _format.value?.cpn
                                    if (second + 20.23f < (player.duration / 1000).toFloat()) {
                                        watchTimeList.add(second + 20.23f)
                                        if (watchTimeUrl != null && cpn != null) {
                                            streamRepository
                                                .updateWatchTime(
                                                    watchTimeUrl,
                                                    watchTimeList,
                                                    cpn,
                                                    queueData.value.data.playlistId,
                                                ).collect {}
                                        }
                                    } else {
                                        watchTimeList.clear()
                                        if (watchTimeUrl != null && cpn != null) {
                                            streamRepository
                                                .updateWatchTimeFull(
                                                    watchTimeUrl,
                                                    cpn,
                                                    queueData.value.data.playlistId,
                                                ).collect {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            jobWatchtime?.join()
        }
    }

    private fun updateNextPreviousTrackAvailability() {
        _controlState.value =
            _controlState.value.copy(
                isNextAvailable = player.hasNextMediaItem(),
                isPreviousAvailable = player.hasPreviousMediaItem(),
            )
    }

    private fun addMediaItemNotSet(
        mediaItem: GenericMediaItem,
        index: Int? = null,
    ) {
        index?.let {
            player.addMediaItem(it, mediaItem)
        } ?: player.addMediaItem(mediaItem)
        if (player.mediaItemCount == 1) {
            player.prepare()
            player.playWhenReady = true
        }
        updateNextPreviousTrackAvailability()
    }

    private fun moveMediaItem(
        fromIndex: Int,
        newIndex: Int,
    ) {
        player.moveMediaItem(fromIndex, newIndex)
        _currentSongIndex.value = player.currentMediaItemIndex
    }

    private fun skipSegment(position: Long) {
        if (position in 0..player.duration) {
            player.seekTo(position)
        } else if (position > player.duration) {
            player.seekToNext()
        }
    }

    @SuppressLint("PrivateResource")
    private fun updateNotification() {
        updateNotificationJob?.cancel()
        updateNotificationJob =
            coroutineScope.launch {
                var id = (player.currentMediaItem?.mediaId ?: "")
                if (id.contains("Video")) {
                    id = id.removePrefix("Video")
                }
                val liked =
                    songRepository
                        .getSongById(id)
                        .singleOrNull()
                        ?.liked ?: false
                _controlState.value = _controlState.value.copy(isLiked = liked)
                onUpdateNotification.invoke(
                    listOf(
                        GenericCommandButton.Like(liked),
                        GenericCommandButton.Shuffle(isShuffled = _controlState.value.isShuffle),
                        GenericCommandButton.Repeat(repeatState = _controlState.value.repeatState),
                        GenericCommandButton.Radio,
                    ),
                )
            }
    }

    override fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob =
            coroutineScope.launch {
                val positionPersistIntervalMs = 5_000L
                var sinceLastPositionSaveMs = 0L
                while (true) {
                    delay(100)
                    _simpleMediaState.value = SimpleMediaState.Progress(player.currentPosition)
                    sinceLastPositionSaveMs += 100
                    if (sinceLastPositionSaveMs >= positionPersistIntervalMs) {
                        sinceLastPositionSaveMs = 0
                        mayBeSaveRecentPosition()
                        lastfmScrobbler.onProgress(player.currentPosition)
                    }
                }
            }
    }

    override fun startBufferedUpdate() {
        bufferedJob?.cancel()
        bufferedJob =
            coroutineScope.launch {
                while (true) {
                    delay(500)
                    _simpleMediaState.value =
                        SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
                    val current = nowPlayingState.value.songEntity
                    if (current?.durationSeconds == 0 && player.duration > 0L) {
                        _nowPlayingState.update {
                            it.copy(
                                songEntity =
                                    current.copy(
                                        durationSeconds = (player.duration / 1000).toInt(),
                                    ),
                            )
                        }
                    }
                }
            }
    }

    override fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    override fun stopBufferedUpdate() {
        bufferedJob?.cancel()
    }

    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.UpdateVolume -> {}
            PlayerEvent.Backward -> player.seekBack()
            PlayerEvent.Forward -> player.seekForward()
            PlayerEvent.PlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                    stopProgressUpdate()
                } else {
                    player.play()
                    startProgressUpdate()
                }
            }
            PlayerEvent.Next -> {
                resetCrossfade()
                player.seekToNext()
            }
            PlayerEvent.Previous -> {
                resetCrossfade()
                player.seekToPrevious()
            }
            PlayerEvent.SkipToPrevious -> {
                resetCrossfade()
                player.seekToPreviousMediaItem()
            }
            PlayerEvent.Stop -> {
                stopProgressUpdate()
                player.stop()
                _nowPlayingState.value = NowPlayingTrackState.initial()
            }
            is PlayerEvent.UpdateProgress -> {
                player.seekTo((player.duration * playerEvent.newProgress / 100).toLong())
            }
            PlayerEvent.Shuffle -> {
                if (player.shuffleModeEnabled) {
                    player.shuffleModeEnabled = false
                    _controlState.value = _controlState.value.copy(isShuffle = false)
                } else {
                    player.shuffleModeEnabled = true
                    _controlState.value = _controlState.value.copy(isShuffle = true)
                }
            }
            PlayerEvent.Repeat -> {
                when (player.repeatMode) {
                    PlayerConstants.REPEAT_MODE_OFF -> {
                        player.repeatMode = PlayerConstants.REPEAT_MODE_ALL
                        _controlState.value = _controlState.value.copy(repeatState = RepeatState.All)
                    }
                    PlayerConstants.REPEAT_MODE_ONE -> {
                        player.repeatMode = PlayerConstants.REPEAT_MODE_OFF
                        _controlState.value = _controlState.value.copy(repeatState = RepeatState.None)
                    }
                    PlayerConstants.REPEAT_MODE_ALL -> {
                        player.repeatMode = PlayerConstants.REPEAT_MODE_ONE
                        _controlState.value = _controlState.value.copy(repeatState = RepeatState.One)
                    }
                    else -> {
                        when (controlState.first().repeatState) {
                            RepeatState.None -> {
                                player.repeatMode = PlayerConstants.REPEAT_MODE_ALL
                                _controlState.value = _controlState.value.copy(repeatState = RepeatState.All)
                            }
                            RepeatState.One -> {
                                player.repeatMode = PlayerConstants.REPEAT_MODE_ALL
                                _controlState.value = _controlState.value.copy(repeatState = RepeatState.All)
                            }
                            RepeatState.All -> {
                                player.repeatMode = PlayerConstants.REPEAT_MODE_ONE
                                _controlState.value = _controlState.value.copy(repeatState = RepeatState.One)
                            }
                        }
                    }
                }
            }
            PlayerEvent.ToggleLike -> toggleLike()
        }
    }

    override fun toggleRadio() {
        coroutineScope.launch {
            val currentSong = nowPlayingState.value.songEntity ?: return@launch
            songRepository
                .getRadioFromEndpoint(
                    YouTubeWatchEndpoint(
                        videoId = currentSong.videoId,
                        playlistId = "RDAMVM${currentSong.videoId}",
                    ),
                ).collectLatest { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null && data.first.isNotEmpty()) -> {
                            setQueueData(
                                QueueData.Data(
                                    listTracks = data.first,
                                    firstPlayedTrack = data.first.first(),
                                    playlistId = "RDAMVM${currentSong.videoId}",
                                    playlistName = "\"${currentSong.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = data.second,
                                ),
                            )
                            clearMediaItems()
                            currentSong.durationSeconds.let {
                                songRepository.updateDurationSeconds(it, currentSong.videoId)
                            }
                            addMediaItem(currentSong.toGenericMediaItem(), playWhenReady = true)
                            loadPlaylistOrAlbum(0)
                        }
                        else -> {
                            Logger.e(TAG, "toggleRadio: ${res.message}")
                        }
                    }
                }
        }
    }

    override fun toggleLike() {
        toggleLikeJob?.cancel()
        toggleLikeJob =
            coroutineScope.launch {
                var id = (player.currentMediaItem?.mediaId ?: "")
                if (id.contains("Video")) {
                    id = id.removePrefix("Video")
                }
                songRepository.updateLikeStatus(
                    id,
                    if (!(controlState.first().isLiked)) 1 else 0,
                )
                delay(200)
            }
    }

    override fun like(liked: Boolean) {
        _controlState.value = _controlState.value.copy(isLiked = liked)
    }

    override fun resetSongAndQueue() {
        player.clearMediaItems()
        _queueData.value = QueueData()
    }

    override fun sleepStart(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob =
            coroutineScope.launch(Dispatchers.Main) {
                var stoppedPlayback = false
                try {
                    if (minutes == Int.MAX_VALUE) {
                        _sleepTimerState.update {
                            it.copy(isDone = false, timeRemaining = -1)
                        }
                        var duration = player.duration
                        while (duration <= 0L) {
                            delay(500)
                            duration = player.duration
                        }
                        val remaining = (duration - player.currentPosition).coerceAtLeast(0L)
                        val fadeMs = sleepFadeDurationMs.coerceAtMost(remaining)
                        val tailMs = sleepFadeTailMs.coerceAtMost(remaining - fadeMs)
                        delay(remaining - fadeMs - tailMs)
                        fadeOutForSleep(fadeMs)
                        delay(tailMs)
                        player.pause()
                        stoppedPlayback = true
                        _sleepTimerState.update {
                            it.copy(isDone = true, timeRemaining = 0)
                        }
                    } else {
                        _sleepTimerState.update {
                            it.copy(isDone = false, timeRemaining = minutes)
                        }
                        var count = minutes
                        while (count > 0) {
                            val isFinalMinute = count == 1
                            delay(
                                if (isFinalMinute) 60 * 1000L - sleepFadeDurationMs - sleepFadeTailMs else 60 * 1000L,
                            )
                            if (isFinalMinute) {
                                fadeOutForSleep(sleepFadeDurationMs)
                                delay(sleepFadeTailMs)
                            }
                            count--
                            _sleepTimerState.update {
                                it.copy(isDone = false, timeRemaining = count)
                            }
                        }
                        player.pause()
                        stoppedPlayback = true
                        _sleepTimerState.update {
                            it.copy(isDone = true, timeRemaining = 0)
                        }
                    }
                } finally {
                    if (!stoppedPlayback) player.sleepFadeFactor = 1f
                }
            }
    }

    private suspend fun fadeOutForSleep(durationMs: Long) {
        if (durationMs <= 0L) return
        val steps = sleepFadeSteps.toLong().coerceAtMost(durationMs).toInt()
        val delayPerStep = (durationMs / steps).coerceAtLeast(1L)
        for (step in 1..steps) {
            val progress = step.toFloat() / steps
            player.sleepFadeFactor = cos(progress * PI / 2).toFloat()
            delay(delayPerStep)
        }
    }

    override fun sleepStop() {
        sleepTimerJob?.cancel()
        _sleepTimerState.value = SleepTimerState(false, 0)
    }

    override fun removeMediaItem(position: Int) {
        player.removeMediaItem(position)
        val temp =
            _queueData.value.data.listTracks
                .toMutableList()
        temp.removeAt(position)
        _queueData.update {
            it.copy(
                data =
                    it.data.copy(
                        listTracks = temp,
                    ),
            )
        }
        _currentSongIndex.value = player.currentMediaItemIndex
    }

    override fun addMediaItem(
        mediaItem: GenericMediaItem,
        playWhenReady: Boolean,
    ) {
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    override fun clearMediaItems() {
        player.clearMediaItems()
    }

    override fun addMediaItemList(mediaItemList: List<GenericMediaItem>) {
        for (mediaItem in mediaItemList) {
            addMediaItemNotSet(mediaItem)
        }
    }

    override fun playMediaItemInMediaSource(index: Int) {
        val i = if (player.shuffleModeEnabled) player.getUnshuffledIndex(index) else index
        player.seekTo(i, 0)
        player.prepare()
        player.playWhenReady = true
    }

    override fun currentSongIndex(): Int = player.currentMediaItemIndex

    override suspend fun swap(
        from: Int,
        to: Int,
    ) {
        moveMediaItem(from, to)
    }

    override fun resetCrossfade() {
        _controlState.update {
            it.copy(
                isCrossfading = false,
            )
        }
    }

    override fun shufflePlaylist(randomTrackIndex: Int) {
        val playlistId = _queueData.value.data.playlistId ?: return
        val firstPlayedTrack = _queueData.value.data.firstPlayedTrack ?: return
        coroutineScope.launch {
            if (playlistId.startsWith(LOCAL_PLAYLIST_ID)) {
                songRepository.insertSong(firstPlayedTrack.toSongEntity()).collect {}
                clearMediaItems()
                firstPlayedTrack.durationSeconds?.let {
                    songRepository.updateDurationSeconds(it, firstPlayedTrack.videoId)
                }
                addMediaItem(firstPlayedTrack.toGenericMediaItem(), playWhenReady = true)
                val longId = playlistId.replace(LOCAL_PLAYLIST_ID, "").toLong()
                val localPlaylist = localPlaylistRepository.getLocalPlaylist(longId).lastOrNull()?.data
                if (localPlaylist != null) {
                    val trackCount = localPlaylist.tracks?.size ?: return@launch
                    val listPosition =
                        (0 until trackCount).toMutableList().apply {
                            remove(randomTrackIndex)
                        }
                    if (listPosition.isEmpty()) return@launch
                    listPosition.shuffle()
                    _queueData.update {
                        it.copy(
                            data =
                                it.data.copy(
                                    continuation = "SHUFFLE0_${fromListIntToString(listPosition)}",
                                ),
                        )
                    }
                    loadMore()
                }
            }
        }
    }

    override fun loadMore() {
        if (queueData.value.queueState == QueueData.StateSource.STATE_INITIALIZING) return
        val playlistId = _queueData.value.data.playlistId ?: return
        val continuation = _queueData.value.data.continuation
        if (continuation != null) {
            if (playlistId.startsWith(LOCAL_PLAYLIST_ID)) {
                coroutineScope.launch {
                    _queueData.update {
                        it.copy(
                            queueState = QueueData.StateSource.STATE_INITIALIZING,
                        )
                    }
                    val longId =
                        try {
                            playlistId.replace(LOCAL_PLAYLIST_ID, "").toLong()
                        } catch (e: NumberFormatException) {
                            return@launch
                        }
                    if (continuation.startsWith("SHUFFLE")) {
                        val regex = Regex("(?<=SHUFFLE)\\d+(?=_)")
                        var offset = regex.find(continuation)?.value?.toInt() ?: return@launch
                        val posString = continuation.removePrefix("SHUFFLE${offset}_")
                        val listPosition = fromStringToListInt(posString) ?: return@launch
                        val theLastLoad = 50 * (offset + 1) >= listPosition.size
                        localPlaylistRepository
                            .getPlaylistPairSongByListPosition(
                                longId,
                                listPosition.subList(50 * offset, if (theLastLoad) listPosition.size else 50 * (offset + 1)),
                            ).singleOrNull()
                            ?.let { pair ->
                                songRepository.getSongsByListVideoId(pair.map { it.songId }).lastOrNull()?.let { songs ->
                                    if (songs.isNotEmpty()) {
                                        delay(300)
                                        loadMoreCatalog(songs.toArrayListTrack())
                                        offset++
                                        _queueData.update {
                                            it.copy(
                                                data =
                                                    it.data.copy(
                                                        continuation =
                                                            if (!theLastLoad) {
                                                                "SHUFFLE${offset}_$posString"
                                                            } else {
                                                                null
                                                            },
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                    } else if (
                        continuation.startsWith(ASC) ||
                        continuation.startsWith(DESC) ||
                        continuation.startsWith(CUSTOM_ORDER) ||
                        continuation.startsWith(TITLE)
                    ) {
                        val filter =
                            if (continuation.startsWith(ASC)) {
                                FilterState.OlderFirst
                            } else if (continuation.startsWith(DESC)) {
                                FilterState.NewerFirst
                            } else if (continuation.startsWith(CUSTOM_ORDER)) {
                                FilterState.CustomOrder
                            } else {
                                FilterState.Title
                            }
                        val converters = Converters()

                        when (filter) {
                            FilterState.NewerFirst, FilterState.OlderFirst -> {
                                val localDateTime =
                                    try {
                                        val timestampString =
                                            if (filter == FilterState.OlderFirst) {
                                                continuation.removePrefix(ASC)
                                            } else {
                                                continuation.removePrefix(DESC)
                                            }
                                        val timestamp = timestampString.toLong()
                                        converters.fromTimestamp(timestamp)
                                            ?: return@launch
                                    } catch (e: Exception) {
                                        return@launch
                                    }
                                localPlaylistRepository
                                    .getPlaylistPairSongByTime(
                                        longId,
                                        filter,
                                        localDateTime,
                                    ).lastOrNull()
                                    ?.let { pair ->
                                        songRepository.getSongsByListVideoId(pair.map { it.songId }).single().let { songs ->
                                            if (songs.isNotEmpty()) {
                                                delay(300)
                                                loadMoreCatalog(songs.toArrayListTrack())
                                                _queueData.update {
                                                    it.copy(
                                                        data =
                                                            it.data.copy(
                                                                continuation =
                                                                    if (filter ==
                                                                        FilterState.OlderFirst
                                                                    ) {
                                                                        ASC +
                                                                            pair.lastOrNull()?.inPlaylist?.let { inPlaylist ->
                                                                                converters.dateToTimestamp(inPlaylist)
                                                                            }
                                                                    } else {
                                                                        DESC +
                                                                            pair.lastOrNull()?.inPlaylist?.let { inPlaylist ->
                                                                                converters.dateToTimestamp(inPlaylist)
                                                                            }
                                                                    },
                                                            ),
                                                    )
                                                }
                                            } else {
                                                _queueData.update {
                                                    it.copy(
                                                        queueState = QueueData.StateSource.STATE_INITIALIZED,
                                                    )
                                                }
                                                reorderShuffledQueue(player.getCurrentMediaTimeLine())
                                            }
                                        }
                                    }
                            }

                            FilterState.Title, FilterState.CustomOrder -> {
                                val offset =
                                    if (filter == FilterState.CustomOrder) {
                                        continuation.removePrefix(CUSTOM_ORDER).toInt()
                                    } else {
                                        continuation.removePrefix(TITLE).toInt()
                                    }
                                localPlaylistRepository
                                    .getPlaylistPairSongByOffset(
                                        longId,
                                        offset,
                                        filter,
                                    ).lastOrNull()
                                    ?.let { pair ->
                                        songRepository.getSongsByListVideoId(pair.map { it.songId }).single().let { songs ->
                                            if (songs.isNotEmpty()) {
                                                delay(300)
                                                loadMoreCatalog(songs.toArrayListTrack())
                                                _queueData.update {
                                                    it.copy(
                                                        data =
                                                            it.data.copy(
                                                                continuation =
                                                                    if (filter ==
                                                                        FilterState.CustomOrder
                                                                    ) {
                                                                        CUSTOM_ORDER + (offset + 1)
                                                                    } else {
                                                                        TITLE + (offset + 1).toString()
                                                                    },
                                                            ),
                                                    )
                                                }
                                            } else {
                                                _queueData.update {
                                                    it.copy(
                                                        queueState = QueueData.StateSource.STATE_INITIALIZED,
                                                    )
                                                }
                                                reorderShuffledQueue(player.getCurrentMediaTimeLine())
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            } else {
                coroutineScope.launch {
                    _queueData.update {
                        it.copy(
                            queueState = QueueData.StateSource.STATE_INITIALIZING,
                        )
                    }
                    songRepository
                        .getContinueTrack(playlistId, continuation)
                        .lastOrNull()
                        .let { response ->
                            val list = response?.first
                            if (list != null) {
                                loadMoreCatalog(list)
                                _queueData.update {
                                    it.copy(
                                        data =
                                            it.data.copy(
                                                continuation = response.second,
                                            ),
                                    )
                                }
                            } else {
                                _queueData.update {
                                    it.copy(
                                        data =
                                            it.data.copy(
                                                continuation = null,
                                            ),
                                    )
                                }
                                if (runBlocking { dataStoreManager.endlessQueue.first() } == TRUE) {
                                    val lastTrack =
                                        queueData.value.data.listTracks
                                            .lastOrNull() ?: return@launch
                                    val radioId = "RDAMVM${lastTrack.videoId}"
                                    if (radioId == queueData.value.data.playlistId) {
                                        return@launch
                                    }
                                    _queueData.update {
                                        it.copy(
                                            data =
                                                it.data.copy(
                                                    playlistId = radioId,
                                                ),
                                            queueState = QueueData.StateSource.STATE_INITIALIZED,
                                        )
                                    }
                                    reorderShuffledQueue(player.getCurrentMediaTimeLine())
                                    getRelated(lastTrack.videoId)
                                }
                            }
                        }
                }
            }
        } else if (runBlocking { dataStoreManager.endlessQueue.first() } == TRUE) {
            val lastTrack =
                queueData.value.data.listTracks
                    .lastOrNull() ?: return
            _queueData.update {
                it.copy(
                    queueState = QueueData.StateSource.STATE_INITIALIZED,
                    data = it.data.copy(playlistId = "RDAMVM${lastTrack.videoId}"),
                )
            }
            reorderShuffledQueue(player.getCurrentMediaTimeLine())
            getRelated(lastTrack.videoId)
        }
    }

    override fun getRelated(videoId: String) {
        if (queueData.value.queueState == QueueData.StateSource.STATE_INITIALIZING) return
        coroutineScope.launch {
            songRepository.getRelatedData(videoId).collect { response ->
                when (response) {
                    is Resource.Success -> {
                        loadMoreCatalog(response.data?.first?.toCollection(arrayListOf()) ?: arrayListOf())
                        _queueData.update {
                            it.copy(
                                data =
                                    it.data.copy(
                                        continuation = response.data?.second,
                                    ),
                            )
                        }
                    }

                    is Resource.Error -> {
                        _queueData.update {
                            it.copy(
                                queueState = QueueData.StateSource.STATE_INITIALIZED,
                                data =
                                    it.data.copy(
                                        continuation = null,
                                    ),
                            )
                        }
                        reorderShuffledQueue(player.getCurrentMediaTimeLine())
                    }
                }
            }
        }
    }

    override fun setQueueData(queueData: QueueData.Data) {
        _queueData.update {
            it.copy(
                data = queueData,
            )
        }
        player.albumTrackIds =
            if (queueData.playlistType == PlaylistType.ALBUM) {
                queueData.listTracks.map { it.videoId }.toSet()
            } else {
                emptySet()
            }
    }

    override fun getCurrentMediaItem(): GenericMediaItem? = player.currentMediaItem

    override suspend fun moveItemUp(position: Int) {
        moveMediaItem(position, position - 1)
        queueData.value.data.listTracks.toMutableList().let { list ->
            val temp = list[position]
            list[position] = list[position - 1]
            list[position - 1] = temp
            _queueData.update {
                it.copy(
                    data = it.data.copy(listTracks = list),
                )
            }
        }
        _currentSongIndex.value = player.currentMediaItemIndex
    }

    override suspend fun moveItemDown(position: Int) {
        moveMediaItem(position, position + 1)
        queueData.value.data.listTracks.toMutableList().let { list ->
            val temp = list[position]
            list[position] = list[position + 1]
            list[position + 1] = temp
            _queueData.update {
                it.copy(
                    data = it.data.copy(listTracks = list),
                )
            }
        }
        _currentSongIndex.value = player.currentMediaItemIndex
    }

    override fun addFirstMediaItemToIndex(
        mediaItem: GenericMediaItem?,
        index: Int,
    ) {
        if (mediaItem != null) {
            moveMediaItem(0, index)
        }
    }

    override fun reset() {
        _queueData.value = QueueData()
    }

    override suspend fun load(
        downloaded: Int,
        index: Int?,
    ) {
        updateCatalog(downloaded, index).let {
            if (index != 0 && index != null) {
                moveMediaItem(0, index)
            }
            updateNextPreviousTrackAvailability()
            _queueData.update {
                it.copy(
                    queueState = QueueData.StateSource.STATE_INITIALIZED,
                )
            }
            reorderShuffledQueue(player.getCurrentMediaTimeLine())
        }
    }

    override suspend fun loadMoreCatalog(
        listTrack: ArrayList<Track>,
        isAddToQueue: Boolean,
    ) {
        _queueData.update {
            it.copy(
                queueState = QueueData.StateSource.STATE_INITIALIZING,
            )
        }
        val catalogMetadata: ArrayList<Track> = arrayListOf()
        for (i in 0 until listTrack.size) {
            val track = listTrack[i]
            var thumbUrl =
                track.thumbnails?.lastOrNull()?.url
                    ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
            thumbUrl = Regex("=w\\d+-h\\d+").replace(thumbUrl, "=w544-h544")
            val artistName: String = track.artists.toListName().connectArtists()
            val isSong =
                (
                    track.thumbnails?.lastOrNull()?.height != 0 &&
                        track.thumbnails?.lastOrNull()?.height == track.thumbnails?.lastOrNull()?.width &&
                        track.thumbnails?.lastOrNull()?.height != null
                    ) &&
                    (
                        !thumbUrl
                            .contains("hq720") &&
                            !thumbUrl
                                .contains("maxresdefault") &&
                            !thumbUrl.contains("sddefault")
                        )
            if (track.artists.isNullOrEmpty()) {
                songRepository
                    .getSongInfo(track.videoId)
                    .lastOrNull()
                    .let { songInfo ->
                        if (songInfo != null) {
                            catalogMetadata.add(
                                track.copy(
                                    artists =
                                        listOf(
                                            Artist(
                                                songInfo.authorId,
                                                songInfo.author ?: "",
                                            ),
                                        ),
                                ),
                            )
                            addMediaItemNotSet(
                                GenericMediaItem(
                                    mediaId = track.videoId,
                                    uri = track.videoId,
                                    metadata =
                                        GenericMediaMetadata(
                                            title = track.title,
                                            artist = songInfo.author ?: "",
                                            albumTitle = track.album?.name,
                                            artworkUri = thumbUrl,
                                            description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                        ),
                                    customCacheKey = track.videoId,
                                ),
                            )
                        } else {
                            val mediaItem =
                                GenericMediaItem(
                                    mediaId = track.videoId,
                                    uri = track.videoId,
                                    metadata =
                                        GenericMediaMetadata(
                                            title = track.title,
                                            artist = "Various Artists",
                                            albumTitle = track.album?.name,
                                            artworkUri = thumbUrl,
                                            description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                        ),
                                    customCacheKey = track.videoId,
                                )
                            addMediaItemNotSet(mediaItem)
                            catalogMetadata.add(
                                track.copy(
                                    artists = listOf(Artist("", "Various Artists")),
                                ),
                            )
                        }
                    }
            } else {
                addMediaItemNotSet(
                    GenericMediaItem(
                        mediaId = track.videoId,
                        uri = track.videoId,
                        metadata =
                            GenericMediaMetadata(
                                title = track.title,
                                artist = artistName,
                                albumTitle = track.album?.name,
                                artworkUri = thumbUrl,
                                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                            ),
                        customCacheKey = track.videoId,
                    ),
                )
                catalogMetadata.add(track)
            }
        }
        if (!player.playWhenReady && isAddToQueue) {
            player.playWhenReady = false
        }
        _queueData.update {
            it
                .copy(
                    queueState = QueueData.StateSource.STATE_INITIALIZED,
                ).addTrackList(catalogMetadata)
        }
        reorderShuffledQueue(player.getCurrentMediaTimeLine())
    }

    override suspend fun updateCatalog(
        downloaded: Int,
        index: Int?,
    ): Boolean {
        _queueData.update {
            it.copy(
                queueState = QueueData.StateSource.STATE_INITIALIZING,
            )
        }
        val tempQueue: ArrayList<Track> = arrayListOf()
        tempQueue.addAll(queueData.value.data.listTracks)
        val chunkedList = tempQueue.chunked(100)
        _queueData.update {
            it.copy(
                data =
                    it.data.copy(
                        listTracks = arrayListOf(),
                    ),
            )
        }
        val current = if (index != null) tempQueue.getOrNull(index) else null
        chunkedList.forEach { list ->
            val catalogMetadata: ArrayList<Track> = arrayListOf()
            for (i in list.indices) {
                val track = list[i]
                if (track == current) continue
                var thumbUrl =
                    track.thumbnails?.lastOrNull()?.url
                        ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
                thumbUrl = Regex("=w\\d+-h\\d+").replace(thumbUrl, "=w544-h544")
                val isSong =
                    (
                        track.thumbnails?.lastOrNull()?.height != 0 &&
                            track.thumbnails?.lastOrNull()?.height == track.thumbnails?.lastOrNull()?.width &&
                            track.thumbnails?.lastOrNull()?.height != null
                        ) &&
                        (
                            !thumbUrl
                                .contains("hq720") &&
                                !thumbUrl
                                    .contains("maxresdefault") &&
                                !thumbUrl.contains("sddefault")
                            )
                if (downloaded == 1) {
                    if (track.artists.isNullOrEmpty()) {
                        songRepository.getSongInfo(track.videoId).lastOrNull().let { songInfo ->
                            if (songInfo != null) {
                                val mediaItem =
                                    GenericMediaItem(
                                        mediaId = track.videoId,
                                        uri = track.videoId,
                                        metadata =
                                            GenericMediaMetadata(
                                                title = track.title,
                                                artist = songInfo.author ?: "",
                                                albumTitle = track.album?.name,
                                                artworkUri = thumbUrl,
                                                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                            ),
                                        customCacheKey = track.videoId,
                                    )
                                addMediaItemNotSet(mediaItem)
                                catalogMetadata.add(
                                    track.copy(
                                        artists =
                                            listOf(
                                                Artist(
                                                    songInfo.authorId,
                                                    songInfo.author ?: "",
                                                ),
                                            ),
                                    ),
                                )
                            } else {
                                val mediaItem =
                                    GenericMediaItem(
                                        mediaId = track.videoId,
                                        uri = track.videoId,
                                        metadata =
                                            GenericMediaMetadata(
                                                title = track.title,
                                                artist = "Various Artists",
                                                albumTitle = track.album?.name,
                                                artworkUri = thumbUrl,
                                                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                            ),
                                        customCacheKey = track.videoId,
                                    )
                                addMediaItemNotSet(mediaItem)
                                catalogMetadata.add(
                                    track.copy(
                                        artists = listOf(Artist("", "Various Artists")),
                                    ),
                                )
                            }
                        }
                    } else {
                        val mediaItem =
                            GenericMediaItem(
                                mediaId = track.videoId,
                                uri = track.videoId,
                                metadata =
                                    GenericMediaMetadata(
                                        title = track.title,
                                        artist = track.artists.toListName().connectArtists(),
                                        albumTitle = track.album?.name,
                                        artworkUri = thumbUrl,
                                        description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                    ),
                                customCacheKey = track.videoId,
                            )
                        addMediaItemNotSet(mediaItem)
                        catalogMetadata.add(track)
                    }
                } else {
                    val artistName: String = track.artists.toListName().connectArtists()
                    if (track.artists.isNullOrEmpty()) {
                        songRepository
                            .getSongInfo(track.videoId)
                            .cancellable()
                            .lastOrNull()
                            .let { songInfo ->
                                if (songInfo != null) {
                                    catalogMetadata.add(
                                        track.copy(
                                            artists =
                                                listOf(
                                                    Artist(
                                                        songInfo.authorId,
                                                        songInfo.author ?: "",
                                                    ),
                                                ),
                                        ),
                                    )
                                    addMediaItemNotSet(
                                        GenericMediaItem(
                                            mediaId = track.videoId,
                                            uri = track.videoId,
                                            metadata =
                                                GenericMediaMetadata(
                                                    title = track.title,
                                                    artist = songInfo.author ?: "",
                                                    albumTitle = track.album?.name,
                                                    artworkUri = thumbUrl,
                                                    description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                                ),
                                            customCacheKey = track.videoId,
                                        ),
                                    )
                                } else {
                                    val mediaItem =
                                        GenericMediaItem(
                                            mediaId = track.videoId,
                                            uri = track.videoId,
                                            metadata =
                                                GenericMediaMetadata(
                                                    title = track.title,
                                                    artist = "Various Artists",
                                                    albumTitle = track.album?.name,
                                                    artworkUri = thumbUrl,
                                                    description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                                ),
                                            customCacheKey = track.videoId,
                                        )
                                    addMediaItemNotSet(mediaItem)
                                    catalogMetadata.add(
                                        track.copy(
                                            artists = listOf(Artist("", "Various Artists")),
                                        ),
                                    )
                                }
                            }
                    } else {
                        addMediaItemNotSet(
                            GenericMediaItem(
                                mediaId = track.videoId,
                                uri = track.videoId,
                                metadata =
                                    GenericMediaMetadata(
                                        title = track.title,
                                        artist = artistName,
                                        albumTitle = track.album?.name,
                                        artworkUri = thumbUrl,
                                        description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                    ),
                                customCacheKey = track.videoId,
                            ),
                        )
                        catalogMetadata.add(track)
                    }
                }
            }
            _queueData.update {
                it.addTrackList(catalogMetadata)
            }
            delay(200)
        }
        if (current != null && index != null) {
            _queueData.update {
                it.addToIndex(current, index)
            }
        }
        return true
    }

    override fun addQueueToPlayer() {
        loadJob?.cancel()
        loadJob =
            coroutineScope.launch {
                load()
            }
    }

    override fun loadPlaylistOrAlbum(index: Int?) {
        loadJob?.cancel()
        loadJob =
            coroutineScope.launch {
                load(index = index)
            }
    }

    override fun currentOrderIndex(): Int =
        if (player.shuffleModeEnabled) {
            queueData.value.data.listTracks.indexOfLast {
                it.videoId == player.currentMediaItem?.mediaId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
            }
        } else {
            currentSongIndex()
        }

    override fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    override suspend fun playNext(track: Track) {
        _queueData.update {
            it.copy(
                queueState = QueueData.StateSource.STATE_INITIALIZING,
            )
        }
        val catalogMetadata: ArrayList<Track> =
            queueData.value.data.listTracks
                .toCollection(arrayListOf())
        var thumbUrl =
            track.thumbnails?.lastOrNull()?.url
                ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
        thumbUrl = Regex("=w\\d+-h\\d+").replace(thumbUrl, "=w544-h544")
        val artistName: String = track.artists.toListName().connectArtists()
        val isSong =
            (
                track.thumbnails?.lastOrNull()?.height != 0 &&
                    track.thumbnails?.lastOrNull()?.height == track.thumbnails?.lastOrNull()?.width &&
                    track.thumbnails?.lastOrNull()?.height != null
                ) &&
                (
                    !thumbUrl
                        .contains("hq720") &&
                        !thumbUrl
                            .contains("maxresdefault") &&
                        !thumbUrl.contains("sddefault")
                    )
        if ((player.currentMediaItemIndex + 1 in 0..queueData.value.data.listTracks.size)) {
            if (track.artists.isNullOrEmpty()) {
                songRepository.getSongInfo(track.videoId).cancellable().lastOrNull().let { songInfo ->
                    if (songInfo != null) {
                        catalogMetadata.add(
                            player.currentMediaItemIndex + 1,
                            track.copy(
                                artists =
                                    listOf(
                                        Artist(
                                            songInfo.authorId,
                                            songInfo.author ?: "",
                                        ),
                                    ),
                            ),
                        )
                        addMediaItemNotSet(
                            GenericMediaItem(
                                mediaId = track.videoId,
                                uri = track.videoId,
                                metadata =
                                    GenericMediaMetadata(
                                        title = track.title,
                                        artist = songInfo.author ?: "",
                                        albumTitle = track.album?.name,
                                        artworkUri = thumbUrl,
                                        description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                    ),
                                customCacheKey = track.videoId,
                            ),
                            player.currentMediaItemIndex + 1,
                        )
                    } else {
                        val mediaItem =
                            GenericMediaItem(
                                mediaId = track.videoId,
                                uri = track.videoId,
                                metadata =
                                    GenericMediaMetadata(
                                        title = track.title,
                                        artist = "Various Artists",
                                        albumTitle = track.album?.name,
                                        artworkUri = thumbUrl,
                                        description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                                    ),
                                customCacheKey = track.videoId,
                            )
                        addMediaItemNotSet(mediaItem, player.currentMediaItemIndex + 1)
                        catalogMetadata.add(
                            player.currentMediaItemIndex + 1,
                            track.copy(
                                artists = listOf(Artist("", "Various Artists")),
                            ),
                        )
                    }
                }
            } else {
                addMediaItemNotSet(
                    GenericMediaItem(
                        mediaId = track.videoId,
                        uri = track.videoId,
                        metadata =
                            GenericMediaMetadata(
                                title = track.title,
                                artist = artistName,
                                albumTitle = track.album?.name,
                                artworkUri = thumbUrl,
                                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                            ),
                        customCacheKey = track.videoId,
                    ),
                    player.currentMediaItemIndex + 1,
                )
                catalogMetadata.add(player.currentMediaItemIndex + 1, track)
            }
        }
        _queueData.update {
            it
                .copy(
                    data =
                        it.data.copy(
                            listTracks = catalogMetadata,
                        ),
                    queueState = QueueData.StateSource.STATE_INITIALIZED,
                )
        }
        reorderShuffledQueue(player.getCurrentMediaTimeLine())
    }

    override suspend fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int?,
    ) {
        val track =
            when (anyTrack) {
                is Track -> anyTrack
                is SongEntity -> anyTrack.toTrack()
                else -> return
            }
        if (track.isExplicit && dataStoreManager.explicitContentEnabled.first() == FALSE) {
            showToast(ToastType.ExplicitContent)
            return
        }

        val isLocalTrack = track.videoId.startsWith("content://") || track.videoId.startsWith("file://")
        if (isLocalTrack) {
            coroutineScope.launch {
                try {
                    val trackList = withContext(Dispatchers.IO) {
                        ArrayList(getDeviceTracksDirectly(context))
                    }
                    val clickedIndex = trackList.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)

                    setQueueData(
                        QueueData.Data(
                            listTracks = if (trackList.isNotEmpty()) trackList else arrayListOf(track),
                            firstPlayedTrack = track,
                            playlistId = "DEVICE_SONGS",
                            playlistName = "Device Songs",
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = null,
                        ),
                    )

                    clearMediaItems()
                    track.durationSeconds?.let {
                        songRepository.updateDurationSeconds(it, track.videoId)
                    }
                    addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
                    loadPlaylistOrAlbum(clickedIndex)
                } catch (e: Exception) {
                    Logger.e(TAG, "Error playing local media: ${e.message}")
                    e.printStackTrace()
                }
            }
            return
        }

        songRepository.insertSong(track.toSongEntity()).singleOrNull()?.let {
            Logger.d(TAG, "Inserted song: ${track.title}")
        }
        clearMediaItems()
        track.durationSeconds?.let {
            songRepository.updateDurationSeconds(it, track.videoId)
        }
        addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)

        when (type) {
            SONG_CLICK, VIDEO_CLICK, SHARE -> {
                getRelated(track.videoId)
            }

            PLAYLIST_CLICK, ALBUM_CLICK, RADIO_CLICK -> {
                loadPlaylistOrAlbum(index)
            }
        }
    }

    override fun getPlayerDuration(): Long = player.duration

    override fun getProgress(): Long = player.currentPosition

    override fun mayBeSaveRecentSong(runBlocking: Boolean) {
        val unit =
            suspend {
                if (dataStoreManager.saveRecentSongAndQueue.first() == TRUE) {
                    val videoId = nowPlayingState.value.songEntity?.videoId
                    if (videoId != null && queueData.value.queueState == QueueData.StateSource.STATE_INITIALIZED) {
                        dataStoreManager.saveRecentSong(
                            videoId,
                            player.contentPosition,
                        )
                        dataStoreManager.setPlaylistFromSaved(queueData.value.data.playlistName ?: "")
                        val temp: ArrayList<Track> = ArrayList()
                        temp.clear()
                        temp.addAll(_queueData.value.data.listTracks)
                        songRepository.recoverQueue(temp)
                    }
                }
            }
        if (runBlocking) {
            runBlocking { unit() }
        } else {
            coroutineScope.launch { unit() }
        }
    }

    private fun mayBeSaveRecentPosition() {
        coroutineScope.launch {
            if (dataStoreManager.saveRecentSongAndQueue.first() == TRUE) {
                val videoId = nowPlayingState.value.songEntity?.videoId ?: return@launch
                dataStoreManager.saveRecentSong(videoId, player.contentPosition)
            }
        }
    }

    override fun mayBeNormalizeVolume() {
        runBlocking {
            normalizeVolume = dataStoreManager.normalizeVolume.first() == TRUE
        }
        if (!normalizeVolume) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            player.volume = 1f
            return
        }

        if (!_castState.value.isRemote && player.audioSessionId != PlayerConstants.AUDIO_SESSION_ID_UNSET) {
            try {
                loudnessEnhancer?.release()
            } catch (_: Exception) {
            }
            try {
                loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        player.currentMediaItem?.mediaId?.let { songId ->
            val videoId =
                if (songId.contains("Video")) {
                    songId.removePrefix("Video")
                } else {
                    songId
                }
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob =
                coroutineScope.launch(Dispatchers.Main) {
                    fun Float?.toMb() = ((this ?: 0f) * 100).toInt()
                    streamRepository
                        .getFormatFlow(videoId)
                        .cancellable()
                        .distinctUntilChanged()
                        .collectLatest { format ->
                            if (format != null) {
                                val loudnessMb =
                                    format.loudnessDb.toMb().let {
                                        if (it !in -2000..2000) {
                                            0
                                        } else {
                                            it
                                        }
                                    }
                                try {
                                    loudnessEnhancer?.setTargetGain(0f.toMb() - loudnessMb)
                                    loudnessEnhancer?.enabled = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                try {
                                    secondLoudnessEnhancer?.setTargetGain(0f.toMb() - loudnessMb)
                                    secondLoudnessEnhancer?.enabled = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                }
        }
    }

    override fun mayBeSavePlaybackState() {
        if (runBlocking { dataStoreManager.saveStateOfPlayback.first() } == TRUE) {
            runBlocking {
                dataStoreManager.recoverShuffleAndRepeatKey(
                    player.shuffleModeEnabled,
                    player.repeatMode,
                )
            }
        }
    }

    override fun mayBeRestoreQueue() {
        coroutineScope.launch {
            if (dataStoreManager.saveRecentSongAndQueue.first() == TRUE) {
                val currentPlayingTrack = songRepository.getSongById(dataStoreManager.recentMediaId.first()).lastOrNull()?.toTrack()
                if (currentPlayingTrack != null) {
                    val savedPosition = dataStoreManager.recentPosition.first().toLongOrNull() ?: 0L
                    val savedTracks =
                        songRepository
                            .getSavedQueue()
                            .singleOrNull()
                            ?.firstOrNull()
                            ?.listTrack
                            .orEmpty()
                    var index = savedTracks.indexOfFirst { it.videoId == currentPlayingTrack.videoId }
                    val listTracks =
                        if (index == -1) {
                            index = 0
                            (listOf(currentPlayingTrack) + savedTracks).toCollection(arrayListOf())
                        } else {
                            savedTracks.toCollection(arrayListOf())
                        }
                    setQueueData(
                        QueueData.Data(
                            listTracks = listTracks,
                            firstPlayedTrack = currentPlayingTrack,
                            playlistId = LOCAL_PLAYLIST_ID_SAVED_QUEUE,
                            playlistName = dataStoreManager.playlistFromSaved.first(),
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = null,
                        ),
                    )
                    addMediaItem(currentPlayingTrack.toGenericMediaItem(), playWhenReady = false)
                    loadPlaylistOrAlbum(index = index)
                    loadJob?.join()
                    resetCrossfade()
                    player.seekTo(index, savedPosition)
                    _simpleMediaState.value = SimpleMediaState.Progress(savedPosition)
                }
            }
        }
    }

    override fun shouldReleaseOnTaskRemoved() =
        runBlocking {
            dataStoreManager.killServiceOnExit.first() == TRUE
        }

    override fun release() {
        try {
            if (discordRPC?.isRpcRunning() == true) {
                discordRPC?.closeRPC()
            }
            discordRPC = null
            mayBeSaveRecentSong(true)
            mayBeSavePlaybackState()
            player.removeListener(this)
            try {
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.release()
                loudnessEnhancer = null
                secondLoudnessEnhancer?.enabled = false
                secondLoudnessEnhancer?.release()
                secondLoudnessEnhancer = null
            } catch (_: Exception) {
            }
            progressJob?.cancel()
            bufferedJob?.cancel()
            sleepTimerJob?.cancel()
            volumeNormalizationJob?.cancel()
            toggleLikeJob?.cancel()
            updateNotificationJob?.cancel()
            loadJob?.cancel()
            songEntityJob?.cancel()
            getSkipSegmentsJob?.cancel()
            getFormatJob?.cancel()
            jobWatchtime?.cancel()
            getDataOfNowPlayingTrackStateJob?.cancel()
            rpcSenderJob?.cancel()
            coroutineScope.cancel()
            backgroundScope.cancel()
        } catch (_: Exception) {
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        val loaded = if (player.bufferedPosition > 0) player.bufferedPosition else 0
        val current = if (player.currentPosition > 0) player.currentPosition else 0
        when (playbackState) {
            PlayerConstants.STATE_IDLE -> _simpleMediaState.value = SimpleMediaState.Initial
            PlayerConstants.STATE_ENDED -> _simpleMediaState.value = SimpleMediaState.Ended
            PlayerConstants.STATE_READY -> _simpleMediaState.value = SimpleMediaState.Ready(player.duration)
            else -> {
                if (current >= loaded) {
                    _simpleMediaState.value = SimpleMediaState.Buffering(player.currentPosition)
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _controlState.value = _controlState.value.copy(isPlaying = isPlaying)
        if (isPlaying) {
            startProgressUpdate()
            nowPlayingState.value.songEntity?.let { updateDiscordRpc(it) }
        } else {
            stopProgressUpdate()
            mayBeSaveRecentSong()
            mayBeSavePlaybackState()
            if (discordRPC?.isRpcRunning() == true) {
                discordRPC?.closeRPC()
            }
        }
        updateNextPreviousTrackAvailability()
    }

    override fun onSeeked(positionMs: Long) {
        if (player.isPlaying) {
            nowPlayingState.value.songEntity?.let { updateDiscordRpc(it) }
        }
    }

    override fun onMediaItemTransition(
        mediaItem: GenericMediaItem?,
        reason: Int,
    ) {
        val lastPlayed = nowPlayingState.value.songEntity
        val currentState = simpleMediaState.value
        if (currentState is SimpleMediaState.Progress && lastPlayed != null && lastPlayed.durationSeconds > 0) {
            mayBeTrackingListeningLocal(lastPlayed, currentState.progress)
        }
        mayBeNormalizeVolume()
        if (mediaItem?.mediaId != _nowPlaying.value?.mediaId) {
            _nowPlaying.value = mediaItem
        }
        if (mediaItem?.mediaId != nowPlayingState.value.mediaItem.mediaId) {
            if (mediaItem != null) {
                getDataOfNowPlayingState(mediaItem)
            } else {
                _nowPlayingState.update {
                    NowPlayingTrackState.initial()
                }
            }
        } else if (mediaItem != null) {
            nowPlayingState.value.songEntity?.let { updateDiscordRpc(it) }
        }
        queueData.value.data.listTracks.let { list ->
            if ((list.size > 3 || runBlocking { dataStoreManager.endlessQueue.first() == TRUE }) &&
                list.size - player.currentMediaItemIndex < 3 &&
                list.size - player.currentMediaItemIndex >= 0 &&
                queueData.value.queueState == QueueData.StateSource.STATE_INITIALIZED
            ) {
                loadMore()
            }
        }
        updateNextPreviousTrackAvailability()
        updateNotification()
        if (player.currentMediaItemIndex == 0) {
            resetCrossfade()
        }
        mayBeSaveRecentSong()
    }

    private fun mayBeTrackingListeningLocal(
        song: SongEntity,
        currentPositionMillis: Long,
    ) {
        coroutineScope.launch {
            if (dataStoreManager.localTrackingEnabled.first() != TRUE) return@launch
            val percent = (currentPositionMillis / (song.durationSeconds * 1000f))
            if (percent < 0.2f) return@launch
            analyticsRepository
                .insertPlaybackEvent(
                    videoId = song.videoId,
                    channelIds = song.artistId ?: emptyList(),
                    albumBrowseId = song.albumId,
                    durationSecond = song.durationSeconds.toLong(),
                    listenedSecond = if (percent >= 0.8f) song.durationSeconds.toLong() else (currentPositionMillis / 1000),
                ).collect {}
        }
    }

    private fun updateDiscordRpc(song: SongEntity) {
        coroutineScope.launch {
            val seq = rpcEventSeq.incrementAndGet()
            val snapshot =
                RpcSnapshot(
                    song = song,
                    progressMs = getProgress(),
                    durationMs = getPlayerDuration(),
                    speed = dataStoreManager.playbackSpeed.first(),
                    seq = seq,
                )
            rpcSnapshotFlow.update { cur -> if (cur == null || seq >= cur.seq) snapshot else cur }
        }
    }

    override fun onTracksChanged(tracks: GenericTracks) {}

    override fun onPlayerError(error: PlayerError) {
        pushPlayerError(error)
        if (isAppInForeground()) {
            showToast(ToastType.PlayerError(error.errorCodeName))
        }
        player.pause()
    }

    override fun onShuffleModeEnabledChanged(
        shuffleModeEnabled: Boolean,
        list: List<GenericMediaItem>,
    ) {
        _controlState.value = _controlState.value.copy(isShuffle = shuffleModeEnabled)
        reorderShuffledQueue(list)
        updateNextPreviousTrackAvailability()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNextPreviousTrackAvailability()
        when (repeatMode) {
            PlayerConstants.REPEAT_MODE_OFF -> _controlState.value = _controlState.value.copy(repeatState = RepeatState.None)
            PlayerConstants.REPEAT_MODE_ONE -> _controlState.value = _controlState.value.copy(repeatState = RepeatState.One)
            PlayerConstants.REPEAT_MODE_ALL -> _controlState.value = _controlState.value.copy(repeatState = RepeatState.All)
        }
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        if (isLoading) {
            startBufferedUpdate()
            if (player.bufferedPosition > player.currentPosition) {
                _simpleMediaState.value = SimpleMediaState.Ready(player.duration)
            } else {
                _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
            }
        } else {
            stopBufferedUpdate()
            _simpleMediaState.value = SimpleMediaState.Ready(player.duration)
        }
    }

    override fun onCrossfadeStateChanged(isCrossfading: Boolean) {
        _controlState.update {
            it.copy(isCrossfading = isCrossfading)
        }
    }

    override fun onTimelineChanged(
        list: List<GenericMediaItem>,
        reason: String,
    ) {
        super.onTimelineChanged(list, reason)
        reorderShuffledQueue(list)
    }

    override fun onCastStateChanged(castState: GenericCastState) {
        _castState.value = castState
    }

    private fun reorderShuffledQueue(list: List<GenericMediaItem>) {
        val listTrack = queueData.value.data.listTracks
        if (list.isEmpty()) return
        list
            .mapNotNull {
                listTrack.firstOrNull { track -> track.videoId == it.mediaId }
            }.let { sorted ->
                if (sorted.size != listTrack.size) return
                _queueData.update {
                    it.copy(
                        data = it.data.copy(listTracks = sorted),
                    )
                }
            }
    }
}

private fun getDeviceTracksDirectly(context: Context): List<Track> {
    val trackList = mutableListOf<Track>()
    try {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(uri, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                ).toString()
                val title = cursor.getString(titleCol) ?: "Unknown Track"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val durationMs = cursor.getLong(durationCol)
                val durSec = (durationMs / 1000).toInt()
                val durString = "${durSec / 60}:${String.format("%02d", (durSec % 60).coerceAtLeast(0))}"

                trackList.add(
                    Track(
                        videoId = contentUri,
                        title = title,
                        artists = listOf(Artist("", artist)),
                        duration = durString,
                        durationSeconds = durSec,
                        thumbnails = emptyList(),
                        album = null,
                        isAvailable = true,
                        isExplicit = false,
                        likeStatus = "INDIFFERENT",
                        videoType = "MUSIC_VIDEO_TYPE_ATV",
                        category = null,
                        feedbackTokens = null,
                        resultType = null,
                    ),
                )
            }
        }
    } catch (e: Exception) {
        Logger.e("MediaServiceHandlerImpl", "Error querying device tracks: ${e.message}")
    }
    return trackList
}

private fun isAppInForeground(): Boolean {
    val appProcessInfo = RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(appProcessInfo)
    return appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}