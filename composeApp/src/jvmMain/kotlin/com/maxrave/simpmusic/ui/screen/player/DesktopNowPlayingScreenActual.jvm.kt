package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.Composable
import com.maxrave.simpmusic.DesktopNowPlayingScreen

@Composable
actual fun PlatformDesktopNowPlayingScreen(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    lyricsList: List<String>,
    activeLyricIndex: Int,
    volume: Float,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onLikeClick: () -> Unit,
) {
    DesktopNowPlayingScreen(
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        lyricsList = lyricsList,
        activeLyricIndex = activeLyricIndex,
        volume = volume,
        onBackClick = onBackClick,
        onPlayPauseClick = onPlayPauseClick,
        onNextClick = onNextClick,
        onPrevClick = onPrevClick,
        onSeek = onSeek,
        onVolumeChange = onVolumeChange,
        onLikeClick = onLikeClick,
    )
}