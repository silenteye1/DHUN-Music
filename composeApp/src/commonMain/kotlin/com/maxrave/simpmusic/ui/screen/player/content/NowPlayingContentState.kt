package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.extension.GradientOffset
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.flow.StateFlow

internal fun NowPlayingScreenData.LyricsData?.canVote(): Boolean {
    val data = this ?: return false
    val votableLyrics =
        data.lyricsProvider == LyricsProvider.SIMPMUSIC && data.lyrics.simpMusicLyrics != null
    val votableTranslation =
        data.translatedLyrics?.second == LyricsProvider.SIMPMUSIC &&
            data.translatedLyrics?.first?.simpMusicLyrics != null
    return votableLyrics || votableTranslation
}

internal val PlayerBackdropColor = Color(0xFF121212)

private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")
private val WHITESPACE_REGEX = Regex("""\s+""")

internal fun String.stripRichSyncTimestamps(): String =
    replace(RICH_SYNC_TIMESTAMP_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

internal fun String?.toAudioCodecLabel(): String? {
    val codec = this ?: return null
    return when {
        codec.contains("opus", ignoreCase = true) -> "OPUS"
        codec.contains("mp4a", ignoreCase = true) || codec.contains("aac", ignoreCase = true) -> "AAC"
        else -> null
    }
}

@Stable
class NowPlayingContentState(
    val screenData: NowPlayingScreenData,
    val controllerState: ControlState,
    val timelineState: TimeLine,
    val timelineFlow: StateFlow<TimeLine>,
    val likeStatus: Boolean,
    val castState: GenericCastState,
    val shouldShowVideo: Boolean,
    val isUserLoggedIn: Boolean,
    val artworkQueue: List<Track>,
    val currentOrderIndex: Int,
    val artworkPagerState: PagerState,
    val startColor: Animatable<Color, AnimationVector4D>,
    val endColor: Animatable<Color, AnimationVector4D>,
    val spotShadowColor: Color,
    val gradientOffset: GradientOffset,
    val sliderTrackColor: Color,
    val sliderValue: Float,
    val currentLyricLineIndex: Int,
    val showControlLayout: Boolean,
    val controlLayoutAlpha: Float,
    val showHideMiddleLayout: Boolean,
    val shouldShowToolbar: Boolean,
    val isInPipMode: Boolean,
    val mainScrollState: ScrollState,
    val isExpanded: Boolean,
    val dismissIcon: ImageVector,
    val audioCodecLabel: String? = null,
)

@Stable
class NowPlayingContentActions(
    val onUIEvent: (UIEvent) -> Unit,
    val onSeekToQueueIndex: (Int) -> Unit,
    val onArtworkBitmap: (ImageBitmap) -> Unit,
    val onSliderChange: (Float) -> Unit,
    val onSliderChangeFinished: () -> Unit,
    val onToggleControls: () -> Unit,
    val onNavigateToArtist: () -> Unit,
    val onAddToYouTubeLiked: () -> Unit,
    val onShowMoreSheet: () -> Unit,
    val onShowQueue: () -> Unit,
    val onShowInfo: () -> Unit,
    val onShowAddToPlaylist: () -> Unit,
    val onShowFullscreenLyrics: () -> Unit,
    val onShowVoteDialog: () -> Unit,
    val onEnterFullscreenVideo: () -> Unit,
    val onDismiss: () -> Unit,
    val onToolbarVisibilityChange: (Boolean) -> Unit,
    val onMoveQueueItem: (from: Int, to: Int) -> Unit,
    val onRemoveQueueItem: (index: Int) -> Unit,
    val onStartMusicFlow: () -> Unit = {},
)