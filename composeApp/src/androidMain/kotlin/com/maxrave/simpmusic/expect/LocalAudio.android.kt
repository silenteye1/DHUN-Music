package com.maxrave.simpmusic.expect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.simpmusic.utils.LocalAudioScanner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AndroidAudioScannerHelper : KoinComponent {
    val context: Context by inject()
}

actual fun fetchDeviceLocalTracks(): List<SongEntity> {
    return try {
        val context = AndroidAudioScannerHelper.context
        LocalAudioScanner.getLocalAudioTracks(context)
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
actual fun rememberStoragePermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val context = AndroidAudioScannerHelper.context
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    return {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            onResult(true)
        } else {
            launcher.launch(permission)
        }
    }
}