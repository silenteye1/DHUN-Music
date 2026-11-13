package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.entities.SongEntity

object LocalAudioScanner {
    fun getLocalAudioTracks(context: Any? = null): List<SongEntity> {
        // Desktop / JVM target par Android MediaStore unavailable hone ki wajah se empty list return karta hai
        return emptyList()
    }
}