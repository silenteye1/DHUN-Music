package com.maxrave.simpmusic.utils

import androidx.compose.runtime.Composable

sealed class SongRecognitionState {
    data object Idle : SongRecognitionState()
    data object Listening : SongRecognitionState()
    data object Processing : SongRecognitionState()
    data class Success(val title: String, val artist: String) : SongRecognitionState()
    data class Error(val message: String) : SongRecognitionState()
}

interface SongRecognitionController {
    val state: SongRecognitionState
    val amplitude: Float
    fun startListening()
    fun cancel()
    fun reset()
}

@Composable
expect fun rememberSongRecognitionController(
    onSongRecognized: (query: String) -> Unit,
): SongRecognitionController