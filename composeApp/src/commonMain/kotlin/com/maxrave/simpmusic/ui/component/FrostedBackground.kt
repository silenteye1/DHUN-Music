package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun FrostedBackground(
    dominantColor: Color? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val primaryAccent = dominantColor ?: MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidColors")

    val color1 by infiniteTransition.animateColor(
        initialValue = primaryAccent,
        targetValue = Color(0xFF6200EA), // Rich Electric Violet
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "C1",
    )

    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF00B4D8), // Cyan Glow
        targetValue = Color(0xFFFF007F), // Vivid Rose
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "C2",
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FloatAnim",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080911)) // Midnight Navy Base
    ) {
        // Haze Source Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            // Top Orb
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(70.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(color1.copy(alpha = 0.50f), Color.Transparent),
                            center = Offset(300f + floatOffset, 450f + floatOffset),
                            radius = 800f,
                        )
                    )
            )

            // Center / Right Orb
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(color2.copy(alpha = 0.40f), Color.Transparent),
                            center = Offset(850f - floatOffset, 1200f - floatOffset),
                            radius = 900f,
                        )
                    )
            )
        }

        // Screen UI
        content()
    }
}