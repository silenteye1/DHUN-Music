package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * Liquid Glass styling with Settings Toggle Support
 */
fun Modifier.frostedGlass(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(22.dp),
    isDarkTheme: Boolean = true,
    isLiquidGlassEnabled: Boolean = true,
): Modifier {
    return this
        .graphicsLayer {
            this.shape = shape
            clip = true
        }
        .clip(shape)
        .then(
            if (isLiquidGlassEnabled && hazeState != null) {
                // Glass ON: Real-time blur with dynamic gradient borders
                Modifier
                    .hazeEffect(
                        state = hazeState,
                        style =
                            HazeDefaults.style(
                                backgroundColor =
                                    if (isDarkTheme) {
                                        Color.Black.copy(alpha = 0.32f)
                                    } else {
                                        Color.White.copy(alpha = 0.40f)
                                    },
                                blurRadius = 26.dp,
                                noiseFactor = 0.04f,
                            ),
                    )
                    .border(
                        width = 1.dp,
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.08f),
                                    ),
                            ),
                        shape = shape,
                    )
            } else {
                // Glass OFF: Clean solid/subtle dark surface (Zero blur overhead)
                Modifier
                    .background(
                        color =
                            if (isDarkTheme) {
                                Color(0xFF181924)
                            } else {
                                Color(0xFFF1F3F9)
                            },
                        shape = shape,
                    )
                    .border(
                        width = 1.dp,
                        color =
                            if (isDarkTheme) {
                                Color.White.copy(alpha = 0.08f)
                            } else {
                                Color.Black.copy(alpha = 0.08f)
                            },
                        shape = shape,
                    )
            },
        )
}