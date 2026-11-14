package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.model.analytics.AnalyticsPeriodStats
import com.maxrave.domain.data.model.analytics.ListeningFingerprint
import com.maxrave.domain.extension.now
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime

/**
 * The whole Wrapped reel for one calendar year, composed once.
 */
class WrappedViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {
    val uiState: StateFlow<WrappedUiState> =
        flow { emit(composeYear(now().date.year)) }
            .stateIn(viewModelScope, SharingStarted.Lazily, WrappedUiState.Loading)

    private suspend fun composeYear(year: Int): WrappedUiState {
        val (start, end) = yearRange(year)
        val stats = analyticsRepository.getPeriodStats(start, end)

        val (previousStart, previousEnd) = yearRange(year - 1)
        val previousStats =
            analyticsRepository.getPeriodStats(previousStart, previousEnd).takeIf { !it.isEmpty }

        val decadeCoverage = if (stats.plays > 0) stats.datedPlays / stats.plays.toFloat() else 0f
        val showDecades = decadeCoverage >= WrappedYear.DECADE_COVERAGE_FLOOR

        return WrappedUiState.Ready(
            WrappedYear(
                year = year,
                daysInYear = LocalDate(year, 12, 31).dayOfYear,
                stats = stats,
                previousStats = previousStats,
                topTracks = topTracksOf(start, end),
                topArtists = topArtistsOf(start, end, previousStart, previousEnd),
                topAlbums = topAlbumsOf(start, end),
                biggestDay = biggestDayOf(stats),
                clock = clockOf(stats),
                archetype = archetypeOf(stats.fingerprint),
                decades = if (showDecades) decadeSharesOf(stats) else emptyList(),
                showDecades = showDecades,
                decadeCoverage = decadeCoverage,
            ),
        )
    }

    private fun yearRange(year: Int): Pair<LocalDateTime, LocalDateTime> =
        LocalDate(year, 1, 1).atTime(0, 0) to LocalDate(year, 12, 31).atTime(23, 59, 59)

    private suspend fun topTracksOf(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<WrappedTrack> =
        analyticsRepository
            .queryTopPlayedSongsInRange(startTimestamp = start, endTimestamp = end)
            .lastOrNull()
            .orEmpty()
            .resolveTop { songRepository.getSongById(it.videoId).lastOrNull() }
            .mapIndexed { index, (row, song) ->
                WrappedTrack(
                    rank = index + 1,
                    song = song,
                    playCount = row.playCount,
                    listenedSeconds = row.totalListeningTime,
                )
            }

    private suspend fun topArtistsOf(
        start: LocalDateTime,
        end: LocalDateTime,
        previousStart: LocalDateTime,
        previousEnd: LocalDateTime,
    ): List<WrappedArtist> {
        val previousTopChannelId =
            analyticsRepository
                .queryTopArtistsWithTimeInRange(startTimestamp = previousStart, endTimestamp = previousEnd)
                .lastOrNull()
                ?.firstOrNull()
                ?.channelId
        return analyticsRepository
            .queryTopArtistsWithTimeInRange(startTimestamp = start, endTimestamp = end)
            .lastOrNull()
            .orEmpty()
            .resolveTop { resolveArtist(it.channelId) }
            .mapIndexed { index, (row, artist) ->
                WrappedArtist(
                    rank = index + 1,
                    artist = artist,
                    playCount = row.playCount,
                    listenedSeconds = row.totalListeningTime,
                    wasTopArtistLastYear = index == 0 && previousTopChannelId == row.channelId,
                )
            }
    }

    private suspend fun topAlbumsOf(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<WrappedAlbum> =
        analyticsRepository
            .queryTopAlbumsInRange(startTimestamp = start, endTimestamp = end)
            .lastOrNull()
            .orEmpty()
            .resolveTop { albumRepository.getAlbum(it.albumBrowseId).lastOrNull() }
            .mapIndexed { index, (row, album) ->
                WrappedAlbum(rank = index + 1, album = album, playCount = row.playCount)
            }

    private suspend fun biggestDayOf(stats: AnalyticsPeriodStats): WrappedBiggestDay? {
        val date = stats.busiestDay ?: return null
        val top =
            analyticsRepository
                .queryTopPlayedSongsInRange(
                    startTimestamp = date.atTime(0, 0),
                    endTimestamp = date.atTime(23, 59, 59),
                ).lastOrNull()
                ?.firstOrNull()
        return WrappedBiggestDay(
            date = date,
            plays = stats.busiestDayPlays,
            typicalPlays = stats.playsPerActiveDay,
            topTrack = top?.let { songRepository.getSongById(it.videoId).lastOrNull() },
            topTrackPlays = top?.playCount ?: 0,
        )
    }

    private fun clockOf(stats: AnalyticsPeriodStats): WrappedClock {
        val playsByHour = stats.playsByHour
        val total = playsByHour.sum()
        val band =
            WrappedListeningBand.entries.maxByOrNull { it.playsIn(playsByHour) }
                ?: WrappedListeningBand.NIGHT
        return WrappedClock(
            playsByHour = playsByHour,
            peakHour = playsByHour.indices.maxByOrNull { playsByHour[it] } ?: 0,
            band = band,
            bandShare = if (total > 0) band.playsIn(playsByHour) / total.toFloat() else 0f,
        )
    }

    private fun WrappedListeningBand.playsIn(playsByHour: List<Int>): Int =
        playsByHour.subList(startHour, endHour).sum()

    private fun archetypeOf(fingerprint: ListeningFingerprint): WrappedArchetype {
        val axes = fingerprint.axes
        val top = axes.indices.maxByOrNull { axes[it] } ?: 0
        return WrappedArchetype.entries.getOrElse(top) { WrappedArchetype.THE_REGULAR }
    }

    private fun decadeSharesOf(stats: AnalyticsPeriodStats): List<WrappedDecadeShare> {
        if (stats.datedPlays <= 0) return emptyList()
        return stats.decades
            .map {
                WrappedDecadeShare(
                    decade = it.decade,
                    plays = it.plays,
                    share = it.plays / stats.datedPlays.toFloat(),
                )
            }.sortedByDescending { it.share }
    }

    private suspend fun resolveArtist(channelId: String): ArtistEntity? =
        artistRepository.getArtistById(channelId).lastOrNull() ?: fetchArtistFromYouTube(channelId)

    private suspend fun fetchArtistFromYouTube(channelId: String): ArtistEntity? =
        artistRepository
            .getArtistData(channelId)
            .lastOrNull()
            ?.takeIf { it is Resource.Success && it.data != null }
            ?.data
            ?.let {
                val entity =
                    ArtistEntity(
                        channelId = channelId,
                        name = it.name,
                        thumbnails = it.thumbnails?.lastOrNull()?.url,
                        followed = false,
                        followedAt = null,
                        inLibrary = now(),
                    )
                artistRepository.insertArtist(entity)
                entity
            }

    private suspend fun <T, R> List<T>.resolveTop(resolve: suspend (T) -> R?): List<Pair<T, R>> {
        val resolved = mutableListOf<Pair<T, R>>()
        for (row in this) {
            if (resolved.size == TOP_COUNT) break
            val entity = resolve(row) ?: continue
            resolved += row to entity
        }
        return resolved
    }

    companion object {
        private const val TOP_COUNT = 5
    }
}