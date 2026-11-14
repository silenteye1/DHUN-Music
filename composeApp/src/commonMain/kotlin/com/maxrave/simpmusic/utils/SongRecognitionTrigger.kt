package com.maxrave.simpmusic.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SongRecognitionTrigger {
    private val _shouldListen = MutableStateFlow(false)
    val shouldListen: StateFlow<Boolean> = _shouldListen.asStateFlow()

    fun requestListen() {
        _shouldListen.value = true
    }

    fun consumeListenRequest() {
        _shouldListen.value = false
    }
}