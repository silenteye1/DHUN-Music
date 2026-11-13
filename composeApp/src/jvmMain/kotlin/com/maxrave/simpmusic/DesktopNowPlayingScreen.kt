package com.maxrave.simpmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo

@Composable
fun DesktopNowPlayingScreen(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    lyricsList: List<String> = emptyList(),
    activeLyricIndex: Int = 0,
    volume: Float = 0.8f,
    onBackClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPrevClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onLikeClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onLyricClick: (Int) -> Unit = {},
    onQueueClick: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
) {
    val lyricsListState = rememberLazyListState()

    LaunchedEffect(activeLyricIndex) {
        if (activeLyricIndex in lyricsList.indices) {
            lyricsListState.animateScrollToItem(
                index = (activeLyricIndex - 1).coerceAtLeast(0)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B11))
    ) {
        // --- 1. Lightweight Ambient Background ---
        if (!artworkUrl.isNullOrEmpty()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E3A5F).copy(alpha = 0.6f),
                                Color(0xFF0F1E33).copy(alpha = 0.8f),
                                Color(0xFF070B11)
                            ),
                            radius = 1200f
                        )
                    )
            )
        }

        // Dark Vignette Tint
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
        )

        // --- 2. Main Page Layout ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBackClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = SimpIcons.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Back to Library",
                        style = typo().bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Two-Column Section (Left: Cover & Info, Right: Lyrics)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Artwork + Action Pills + Track Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Big Artwork Card
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFF151D28),
                        modifier = Modifier
                            .size(320.dp)
                            .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                            .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
                    ) {
                        AsyncImage(
                            model = artworkUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // Action Buttons (Heart, Share, Options)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassActionButton(text = "♥", onClick = onLikeClick)
                        GlassActionButton(text = "⤤", onClick = onShareClick)
                        GlassActionButton(text = "•••", onClick = onMenuClick)
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = title.ifEmpty { "No Title" },
                        style = typo().titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = artist.ifEmpty { "Unknown Artist" },
                        style = typo().bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Column: Lyrics
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(start = 32.dp, end = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (lyricsList.isEmpty()) {
                        Text(
                            text = "No lyrics available",
                            style = typo().bodyLarge,
                            color = Color.White.copy(alpha = 0.35f),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        LazyColumn(
                            state = lyricsListState,
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                            contentPadding = PaddingValues(vertical = 140.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lyricsList) { index, line ->
                                val isActive = index == activeLyricIndex
                                Text(
                                    text = line,
                                    fontSize = if (isActive) 30.sp else 22.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.30f),
                                    lineHeight = if (isActive) 40.sp else 32.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLyricClick(index) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- 3. Bottom Floating Glass Player Dock ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF202A38).copy(alpha = 0.55f),
                                Color(0xFF101722).copy(alpha = 0.70f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left mini preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF151D28),
                            modifier = Modifier.size(46.dp)
                        ) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                text = title.ifEmpty { "No Title" },
                                style = typo().bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist.ifEmpty { "Unknown Artist" },
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Center controls + seek bar
                    Column(
                        modifier = Modifier.weight(2f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text("🔀", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.clickable { })
                            Text("⏮", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { onPrevClick() })

                            // Center Glowing Play/Pause Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF6B9BFA), Color(0xFF386BE0))
                                        )
                                    )
                                    .shadow(12.dp, CircleShape, spotColor = Color(0xFF4A82F6))
                                    .clickable { onPlayPauseClick() }
                            ) {
                                Text(
                                    text = if (isPlaying) "⏸" else "▶",
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                            }

                            Text("⏭", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { onNextClick() })
                            Text("🔁", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.clickable { })
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Text(
                                text = formatDuration(currentPositionMs),
                                style = typo().labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )

                            Slider(
                                value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f,
                                onValueChange = onSeek,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF7DA7F8),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )

                            Text(
                                text = formatDuration(durationMs),
                                style = typo().labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Right Volume & Extra Icons
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔉", color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.width(90.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White.copy(alpha = 0.85f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(Modifier.width(14.dp))
                        Text("☰", color = Color.White.copy(alpha = 0.75f), modifier = Modifier.clickable { onQueueClick() })
                        Spacer(Modifier.width(14.dp))
                        Text("⛶", color = Color.White.copy(alpha = 0.75f), modifier = Modifier.clickable { onFullscreenClick() })
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassActionButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF222C3A).copy(alpha = 0.45f),
                        Color(0xFF131A24).copy(alpha = 0.60f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 13.sp)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSec = millis / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)
}