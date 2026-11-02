package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Default Dynamic Ambient Mesh Wallpaper:
 * Renders rich ambient color glow across the entire screen when no custom wallpaper is selected.
 */
@Composable
fun DynamicAmbientBackground(
    modifier: Modifier = Modifier,
    primaryAmbientColor: Color = MaterialTheme.colorScheme.primary,
    secondaryAmbientColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    val animatedPrimary by animateColorAsState(
        targetValue = primaryAmbientColor,
        animationSpec = tween(durationMillis = 1000),
        label = "PrimaryAmbientColor",
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryAmbientColor,
        animationSpec = tween(durationMillis = 1000),
        label = "SecondaryAmbientColor",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06090E)) // Base dark atmosphere
            .drawBehind {
                // Top-left ambient orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedPrimary.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.18f),
                        radius = size.maxDimension * 0.70f,
                    ),
                )

                // Middle-right secondary glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedSecondary.copy(alpha = 0.40f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.45f),
                        radius = size.maxDimension * 0.65f,
                    ),
                )

                // Bottom ambient pool
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedPrimary.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.40f, size.height * 0.90f),
                        radius = size.maxDimension * 0.60f,
                    ),
                )
            },
    )
}