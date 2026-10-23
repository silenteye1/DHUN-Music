package com.maxrave.simpmusic

import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.importdata.ExternalPlaylistInfo
import com.maxrave.domain.data.model.importdata.ExternalTrackInfo
import com.maxrave.domain.data.model.importdata.PlaylistLinkHelper
import com.maxrave.domain.data.model.importdata.SupportedPlatform
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed class ImportState {
    object Idle : ImportState()
    object FetchingMetadata : ImportState()
    data class MatchingTracks(val current: Int, val total: Int, val songTitle: String) : ImportState()
    object CreatingYouTubePlaylist : ImportState()
    data class Success(val playlistName: String, val matchedCount: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

class SpotifyPlaylistImporter(
    private val searchRepository: SearchRepository,
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val httpClient: HttpClient = HttpClient(),
) {
    fun importFromUrl(url: String): Flow<ImportState> = flow {
        val platform = PlaylistLinkHelper.detectPlatform(url)
        if (platform != SupportedPlatform.SPOTIFY) {
            emit(ImportState.Error("Currently only public Spotify playlists are supported."))
            return@flow
        }

        val playlistId = PlaylistLinkHelper.extractSpotifyPlaylistId(url)
        if (playlistId.isNullOrBlank()) {
            emit(ImportState.Error("Invalid Spotify playlist URL."))
            return@flow
        }

        emit(ImportState.FetchingMetadata)

        val playlistInfoResult = runCatching { fetchSpotifyEmbedData(playlistId) }
        val playlistInfo = playlistInfoResult.getOrNull()
        if (playlistInfo == null || playlistInfo.tracks.isEmpty()) {
            emit(ImportState.Error("Could not fetch tracks. Ensure the playlist is public."))
            return@flow
        }

        val totalTracks = playlistInfo.tracks.size
        val matchedVideoIds = mutableListOf<String>()
        val songsToInsert = mutableListOf<SongEntity>()
        var processedCount = 0

        // 3-3 ke chunks me batch execute karenge (Fast + Safe from rate-limit & no coroutine violation)
        val batches = playlistInfo.tracks.chunked(3)

        for (batch in batches) {
            // UI Progress emit humesha main flow execution par hoga
            val currentTitle = batch.firstOrNull()?.title ?: ""
            emit(ImportState.MatchingTracks(current = processedCount + 1, total = totalTracks, songTitle = currentTitle))

            val batchResults = coroutineScope {
                batch.map { track ->
                    async(Dispatchers.IO) {
                        findBestMatchingSong(track)
                    }
                }.awaitAll()
            }

            batchResults.filterNotNull().forEach { (videoId, songEntity) ->
                matchedVideoIds.add(videoId)
                songsToInsert.add(songEntity)
            }

            processedCount += batch.size
        }

        if (matchedVideoIds.isEmpty()) {
            emit(ImportState.Error("No matching tracks found on YouTube Music."))
            return@flow
        }

        // DB me songs insert karna
        songsToInsert.forEach { song ->
            runCatching { songRepository.insertSong(song).first() }
        }

        emit(ImportState.CreatingYouTubePlaylist)

        // YouTube Account par playlist create karna
        val createResult = runCatching {
            playlistRepository.createYouTubePlaylist(
                title = "${playlistInfo.name} (Spotify)",
                listVideoId = matchedVideoIds,
            ).first()
        }.getOrNull()

        if (createResult is Resource.Success) {
            emit(ImportState.Success(playlistName = playlistInfo.name, matchedCount = matchedVideoIds.size))
        } else {
            val errorMsg = (createResult as? Resource.Error)?.message ?: "Failed to create playlist on YouTube. Make sure you are logged into YouTube Music."
            emit(ImportState.Error(errorMsg))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun findBestMatchingSong(track: ExternalTrackInfo): Pair<String, SongEntity>? {
        val cleanTitle = track.title
            .replace(Regex("\\(.*?feat.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?feat.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*?remaster.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?remaster.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("- Remastered.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(From \".*?\"\\)", RegexOption.IGNORE_CASE), "")
            .trim()

        val primaryQuery = "$cleanTitle ${track.artist}".trim()

        var searchResult = runCatching { searchRepository.getSearchDataSong(primaryQuery).first() }.getOrNull()

        if ((searchResult !is Resource.Success || searchResult.data.isNullOrEmpty()) && cleanTitle != track.title) {
            searchResult = runCatching { searchRepository.getSearchDataSong("${track.title} ${track.artist}".trim()).first() }.getOrNull()
        }

        if (searchResult !is Resource.Success || searchResult.data.isNullOrEmpty()) {
            searchResult = runCatching { searchRepository.getSearchDataSong(cleanTitle).first() }.getOrNull()
        }

        if (searchResult is Resource.Success && !searchResult.data.isNullOrEmpty()) {
            val matchedSong = searchResult.data?.firstOrNull()
            val videoId = matchedSong?.videoId
            if (!videoId.isNullOrBlank()) {
                val artistNames = matchedSong.artists?.map { it.name } ?: listOf(track.artist)
                val artistIds = matchedSong.artists?.mapNotNull { it.id }

                val songEntity = SongEntity(
                    videoId = videoId,
                    title = matchedSong.title ?: track.title,
                    artistName = artistNames,
                    artistId = if (!artistIds.isNullOrEmpty()) artistIds else null,
                    albumId = matchedSong.album?.id,
                    albumName = matchedSong.album?.name,
                    duration = matchedSong.duration ?: "",
                    durationSeconds = track.durationSeconds.toInt(),
                    thumbnails = matchedSong.thumbnails?.lastOrNull()?.url ?: "",
                    isAvailable = true,
                    isExplicit = matchedSong.isExplicit ?: false,
                    likeStatus = "",
                    videoType = "",
                    category = null,
                    resultType = null,
                    liked = false,
                    totalPlayTime = 0,
                    downloadState = 0,
                )
                return Pair(videoId, songEntity)
            }
        }
        return null
    }

    private suspend fun fetchSpotifyEmbedData(playlistId: String): ExternalPlaylistInfo {
        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
        val response = httpClient.get(embedUrl) {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        }
        val html = response.bodyAsText()

        val titleMatch = Regex("\"name\":\"(.*?)\"").find(html)
        val playlistName = titleMatch?.groupValues?.getOrNull(1) ?: "Spotify Playlist"

        val trackPattern = Regex("\\{\"uri\":\".*?\",\"title\":\"(.*?)\",\"subtitle\":\"(.*?)\".*?\"duration\":(\\d+)")
        val trackList = mutableListOf<ExternalTrackInfo>()

        trackPattern.findAll(html).forEach { matchResult ->
            val title = matchResult.groupValues.getOrNull(1).orEmpty()
            val artist = matchResult.groupValues.getOrNull(2).orEmpty()
            val durationMs = matchResult.groupValues.getOrNull(3)?.toLongOrNull() ?: 0L

            if (title.isNotBlank()) {
                trackList.add(
                    ExternalTrackInfo(
                        title = unescapeJson(title),
                        artist = unescapeJson(artist),
                        durationSeconds = durationMs / 1000,
                    )
                )
            }
        }

        return ExternalPlaylistInfo(
            name = unescapeJson(playlistName),
            platform = SupportedPlatform.SPOTIFY,
            tracks = trackList,
        )
    }

    private fun unescapeJson(text: String): String {
        return text
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\u0027", "'")
    }
}