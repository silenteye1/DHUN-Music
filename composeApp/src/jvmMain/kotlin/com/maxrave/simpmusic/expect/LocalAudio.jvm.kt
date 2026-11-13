package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.maxrave.domain.data.entities.SongEntity

actual fun fetchDeviceLocalTracks(): List<SongEntity> {
    return emptyList()
}

@Composable
actual fun rememberStoragePermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    return remember {
        { onResult(true) }
    }
}