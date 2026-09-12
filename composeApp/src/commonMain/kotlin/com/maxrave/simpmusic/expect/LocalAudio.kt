package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable
import com.maxrave.domain.data.entities.SongEntity

expect fun fetchDeviceLocalTracks(): List<SongEntity>

@Composable
expect fun rememberStoragePermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit