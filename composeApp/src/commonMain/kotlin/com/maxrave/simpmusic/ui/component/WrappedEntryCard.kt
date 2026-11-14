package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.ui.screen.home.wrapped.wholeMinutes
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_entry_subtitle

@Composable
fun WrappedEntryCard(
    wrapped: WrappedYear,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(CARD_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GUTTER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = wrapped.entryCover(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(COVER_SIZE).clip(RoundedCornerShape(COVER_RADIUS)),
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Dhun Wrapped ${wrapped.year}",
                    style = typo().titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            Res.string.wrapped_entry_subtitle,
                            formatCount(wholeMinutes(wrapped.stats.listenedSeconds)),
                            formatCount(wrapped.stats.distinctArtists),
                        ),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DISC_SIZE),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = SimpIcons.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(DISC_GLYPH_SIZE),
                    )
                }
            }
        }
    }
}

private fun WrappedYear.entryCover(): String? {
    val cover =
        topTracks.firstOrNull()?.song?.thumbnails
            ?: topAlbums.firstOrNull()?.album?.thumbnails
            ?: topArtists.firstOrNull()?.artist?.thumbnails
    return cover?.takeIf { it.isNotBlank() }
}

private val GUTTER = 14.dp
private val COVER_SIZE = 68.dp
private val COVER_RADIUS = 8.dp
private val CARD_RADIUS = 12.dp
private val DISC_SIZE = 44.dp
private val DISC_GLYPH_SIZE = 22.dp