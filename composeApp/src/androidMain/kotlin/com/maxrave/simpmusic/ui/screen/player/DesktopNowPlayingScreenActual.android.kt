package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.Composable

@Suppress("UNUSED_PARAMETER")
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
}