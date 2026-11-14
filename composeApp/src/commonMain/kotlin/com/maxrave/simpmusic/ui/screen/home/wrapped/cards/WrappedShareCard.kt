package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.icon.Download
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.ui.screen.home.wrapped.wholeMinutes
import com.maxrave.simpmusic.viewModel.WrappedArchetype
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.mono
import simpmusic.composeapp.generated.resources.wrapped_archetype_deep_diver
import simpmusic.composeapp.generated.resources.wrapped_archetype_devotee
import simpmusic.composeapp.generated.resources.wrapped_archetype_explorer
import simpmusic.composeapp.generated.resources.wrapped_archetype_omnivore
import simpmusic.composeapp.generated.resources.wrapped_archetype_regular
import simpmusic.composeapp.generated.resources.wrapped_save
import simpmusic.composeapp.generated.resources.wrapped_share
import simpmusic.composeapp.generated.resources.wrapped_share_artists
import simpmusic.composeapp.generated.resources.wrapped_share_card_title
import simpmusic.composeapp.generated.resources.wrapped_share_days
import simpmusic.composeapp.generated.resources.wrapped_share_minutes
import simpmusic.composeapp.generated.resources.wrapped_share_title
import simpmusic.composeapp.generated.resources.wrapped_share_top_album
import simpmusic.composeapp.generated.resources.wrapped_share_top_artists
import simpmusic.composeapp.generated.resources.wrapped_share_top_tracks
import simpmusic.composeapp.generated.resources.wrapped_type_the
import kotlin.math.ceil

@Composable
fun WrappedShareCard(
    wrapped: WrappedYear,
    onSave: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(CARD_TOP_GAP))
        WrappedEyebrow(
            text = stringResource(Res.string.wrapped_share_title),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WrappedTokens.ScreenPadding),
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val posterHeight = min(maxHeight, maxWidth / POSTER_ASPECT)
            SharePoster(
                wrapped = wrapped,
                modifier =
                    posterModifier
                        .height(posterHeight)
                        .aspectRatio(POSTER_ASPECT),
            )
        }

        ShareActions(onSave = onSave, onShare = onShare)
    }
}

@Composable
private fun SharePoster(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    val ground = MaterialTheme.colorScheme.background

    Box(modifier.clipToBounds().background(ground)) {
        PosterMosaic(
            urls = wrapped.posterArtwork(),
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.00f to ground.copy(alpha = 0.58f),
                    0.38f to ground.copy(alpha = 0.88f),
                    1.00f to ground.copy(alpha = 0.96f),
                ),
            ),
        )
        Column(
            modifier = Modifier.matchParentSize().padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PosterHeader(wrapped.year)
            PosterRankings(wrapped)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            PosterFigures(wrapped)
            wrapped.topAlbums.firstOrNull()?.let { album ->
                PosterField(
                    label = stringResource(Res.string.wrapped_share_top_album),
                    value = album.album.title,
                )
            }
            Spacer(Modifier.weight(1f))
            PosterSignature(wrapped)
        }
    }
}

@Composable
private fun PosterHeader(year: Int) {
    Column {
        PosterLabel(
            text = stringResource(Res.string.wrapped_share_card_title).uppercase(),
            tracking = HEADER_TRACKING,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$year",
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = POSTER_YEAR_SIZE,
                    lineHeight = 1.em,
                    letterSpacing = (-0.03).em,
                ),
        )
    }
}

@Composable
private fun PosterRankings(wrapped: WrappedYear) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RankedColumn(
            title = stringResource(Res.string.wrapped_share_top_tracks),
            entries = wrapped.topTracks.map { it.song.title },
            modifier = Modifier.weight(1f),
        )
        RankedColumn(
            title = stringResource(Res.string.wrapped_share_top_artists),
            entries = wrapped.topArtists.map { it.artist.name },
            modifier = Modifier.width(ARTIST_COLUMN_WIDTH),
        )
    }
}

@Composable
private fun RankedColumn(
    title: String,
    entries: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PosterLabel(text = title, tracking = LABEL_TRACKING)
        Spacer(Modifier.height(6.dp))
        entries.take(POSTER_RANK_LIMIT).forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth().height(RANK_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.width(RANK_NUMBER_WIDTH),
                )
                Text(
                    text = entry,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PosterFigures(wrapped: WrappedYear) {
    val stats = wrapped.stats
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        PosterFigure(
            value = formatCount(wholeMinutes(stats.listenedSeconds)),
            label = stringResource(Res.string.wrapped_share_minutes),
        )
        PosterFigure(
            value = formatCount(stats.distinctArtists),
            label = stringResource(Res.string.wrapped_share_artists),
        )
        PosterFigure(
            value = formatCount(stats.activeDays),
            label = stringResource(Res.string.wrapped_share_days),
        )
    }
}

@Composable
private fun PosterFigure(
    value: String,
    label: String,
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        PosterLabel(text = label, tracking = FIGURE_LABEL_TRACKING)
    }
}

@Composable
private fun PosterField(
    label: String,
    value: String,
) {
    Column {
        PosterLabel(text = label, tracking = LABEL_TRACKING)
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PosterLabel(
    text: String,
    tracking: TextUnit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = tracking),
        maxLines = 1,
    )
}

@Composable
private fun PosterSignature(wrapped: WrappedYear) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = archetypeTitle(wrapped),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.mono),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(10.dp),
                )
            }
            Text(
                text = "Dhun Music",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ShareActions(
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(WrappedTokens.FooterHeight),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Button(
            onClick = onSave,
            shape = CircleShape,
            contentPadding = ACTION_CONTENT_PADDING,
        ) {
            ActionContent(SimpIcons.Download, stringResource(Res.string.wrapped_save))
        }
        FilledTonalButton(
            onClick = onShare,
            shape = CircleShape,
            contentPadding = ACTION_CONTENT_PADDING,
        ) {
            ActionContent(SimpIcons.Share, stringResource(Res.string.wrapped_share))
        }
    }
}

@Composable
private fun ActionContent(
    icon: ImageVector,
    label: String,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = LocalContentColor.current,
        maxLines = 1,
    )
}

@Composable
private fun PosterMosaic(
    urls: List<String>,
    modifier: Modifier = Modifier,
) {
    if (urls.isEmpty()) return
    BoxWithConstraints(modifier) {
        val tile = maxWidth / POSTER_MOSAIC_COLUMNS
        val fittingRows = ceil(maxHeight / tile).toInt()
        val coverRows = ceil(urls.size.toFloat() / POSTER_MOSAIC_COLUMNS).toInt()
        Column {
            repeat(minOf(fittingRows, coverRows).coerceAtLeast(1)) { row ->
                Row {
                    repeat(POSTER_MOSAIC_COLUMNS) { column ->
                        val cover = urls.getOrNull(row * POSTER_MOSAIC_COLUMNS + column)
                        if (cover == null) {
                            Spacer(Modifier.size(tile))
                        } else {
                            WrappedArtwork(url = cover, modifier = Modifier.size(tile))
                        }
                    }
                }
            }
        }
    }
}

private fun WrappedYear.posterArtwork(): List<String> {
    val covers =
        topTracks.map { it.song.thumbnails } +
            topAlbums.map { it.album.thumbnails } +
            topArtists.map { it.artist.thumbnails }
    return covers.mapNotNull { url -> url?.takeIf { it.isNotBlank() } }.distinctByArtwork()
}

@Composable
private fun archetypeTitle(wrapped: WrappedYear): String {
    val article = stringResource(Res.string.wrapped_type_the)
    return "$article ${stringResource(wrapped.archetype.posterNameRes())}"
}

private fun WrappedArchetype.posterNameRes(): StringResource =
    when (this) {
        WrappedArchetype.THE_REGULAR -> Res.string.wrapped_archetype_regular
        WrappedArchetype.THE_EXPLORER -> Res.string.wrapped_archetype_explorer
        WrappedArchetype.THE_OMNIVORE -> Res.string.wrapped_archetype_omnivore
        WrappedArchetype.THE_DEVOTEE -> Res.string.wrapped_archetype_devotee
        WrappedArchetype.THE_DEEP_DIVER -> Res.string.wrapped_archetype_deep_diver
    }

private const val POSTER_ASPECT = 268f / 477f
private const val POSTER_MOSAIC_COLUMNS = 3
private const val POSTER_RANK_LIMIT = 5
private val ARTIST_COLUMN_WIDTH = 88.dp
private val RANK_ROW_HEIGHT = 20.dp
private val RANK_NUMBER_WIDTH = 14.dp
private val POSTER_YEAR_SIZE = 34.sp
private val HEADER_TRACKING = 0.22.em
private val LABEL_TRACKING = 0.16.em
private val FIGURE_LABEL_TRACKING = 0.10.em
private val ACTION_CONTENT_PADDING = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
private val CARD_TOP_GAP = 14.dp