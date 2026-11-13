package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class DesktopDeviceVolumeController : DeviceVolumeController {
    private var _volumeFraction by mutableFloatStateOf(0.8f)

    override val volumeFraction: Float
        get() = _volumeFraction

    override fun setVolumeFraction(fraction: Float) {
        _volumeFraction = fraction.coerceIn(0f, 1f)
    }
}

@Composable
actual fun rememberDeviceVolumeController(): DeviceVolumeController? {
    return remember { DesktopDeviceVolumeController() }
}