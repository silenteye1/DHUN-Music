package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * 120Hz Hardware-Accelerated Glass Modifier
 */
fun Modifier.frostedGlass(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(22.dp),
): Modifier {
    return this
        // GPU layer caching taaki scroll lag na kare
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            clip = true
            this.shape = shape
        }
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.35f),
            spotColor = Color.Black.copy(alpha = 0.35f),
        )
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(hazeState) {
                    blurRadius = 20.dp
                    tints = emptyList()
                }
            } else {
                Modifier
            }
        )
        // High-end Specular Base
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1C1D29).copy(alpha = 0.42f),
                    Color(0xFF0F1019).copy(alpha = 0.55f),
                )
            ),
            shape = shape,
        )
        // Chamfer hairline border
        .border(
            width = 0.8.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.05f),
                )
            ),
            shape = shape,
        )
}