@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.PlatformCastButton
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.isElementVisible
import com.maxrave.simpmusic.extension.parseTimestampToMilliseconds
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AIBadge
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.PlayerControlLayout
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.AddCircleOutline
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.icon.ThumbsUpDown
import com.maxrave.simpmusic.ui.theme.blackMoreOverlay
import com.maxrave.simpmusic.ui.theme.overlay
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.like_and_dislike
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_simpmusic
import simpmusic.composeapp.generated.resources.lyrics_provider_youtube
import simpmusic.composeapp.generated.resources.now_playing_upper
import simpmusic.composeapp.generated.resources.offline_mode
import simpmusic.composeapp.generated.resources.playing_on_device
import simpmusic.composeapp.generated.resources.published_at
import simpmusic.composeapp.generated.resources.rate_lyrics
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.share_lyrics
import simpmusic.composeapp.generated.resources.show
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced
import simpmusic.composeapp.generated.resources.view_count

@OptIn(ExperimentalFoundationApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun NowPlayingContentSpotify(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val screenInfo = getScreenSizeInfo()
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One
    val hazeState = rememberHazeState(blurEnabled = true)

    var showShareLyricsSheet by rememberSaveable { mutableStateOf(false) }

    var topAppBarHeightDp by rememberSaveable { mutableIntStateOf(0) }
    var middleLayoutHeightDp by rememberSaveable { mutableIntStateOf(0) }
    var infoLayoutHeightDp by rememberSaveable { mutableIntStateOf(0) }
    var middleLayoutPaddingDp by rememberSaveable { mutableIntStateOf(0) }

    // Guaranteed clearance for Android 3-button navigation bar (prevents button overlap)
    val navBarBottomPaddingDp = with(localDensity) {
        val detected = WindowInsets.navigationBars.getBottom(localDensity).toDp()
        if (detected > 24.dp) detected + 6.dp else 56.dp
    }

    LaunchedEffect(
        topAppBarHeightDp,
        screenInfo,
        infoLayoutHeightDp,
        navBarBottomPaddingDp,
    ) {
        if (topAppBarHeightDp > 0 && middleLayoutHeightDp > 0 && infoLayoutHeightDp > 0 && screenInfo.hDP > 0) {
            val available = screenInfo.hDP - topAppBarHeightDp - middleLayoutHeightDp - infoLayoutHeightDp - navBarBottomPaddingDp.value.toInt()
            val result = available / 2
            middleLayoutPaddingDp = if (result > 6) result else 6
        }
    }

    var showHideFullscreenOverlay by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = showHideFullscreenOverlay) {
        if (showHideFullscreenOverlay) {
            delay(3000)
            showHideFullscreenOverlay = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .verticalScroll(
                    state.mainScrollState,
                    enabled = state.isExpanded,
                )
                .then(
                    if (state.showHideMiddleLayout) {
                        Modifier
                            .background(PlayerBackdropColor)
                            .drawBehind {
                                val gradientHeight = screenInfo.hPX.toFloat()
                                val area = Size(size.width, gradientHeight)
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            state.startColor.value,
                                            state.endColor.value,
                                        ),
                                        start = state.gradientOffset.start,
                                        end = state.gradientOffset.end,
                                    ),
                                    size = area,
                                )
                                drawRect(
                                    brush = smoothScrimBrush(
                                        from = PlayerBackdropColor.copy(alpha = 0f),
                                        to = PlayerBackdropColor,
                                        startY = 0f,
                                        endY = gradientHeight * 0.95f,
                                    ),
                                    size = area,
                                )
                            }
                    } else {
                        Modifier.background(Color.Black)
                    },
                ),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = state.artworkPagerState,
                    modifier = Modifier
                        .height(screenInfo.hDP.dp)
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isRepeatOne && state.artworkQueue.isNotEmpty(),
                    key = { idx ->
                        val vid = state.artworkQueue.getOrNull(idx)?.videoId.orEmpty()
                        "artwork_${vid}_$idx"
                    },
                ) { page ->
                    val pageTrack = state.artworkQueue.getOrNull(page)
                    val isCurrentArtworkPage = page == state.currentOrderIndex
                    val pageHasCanvas = isCurrentArtworkPage && state.screenData.canvasData != null

                    val pagePaletteState = rememberPaletteState()
                    val pageStartColor = remember(pageTrack?.videoId) { Animatable(Color.Black) }
                    LaunchedEffect(pagePaletteState, pageTrack?.videoId) {
                        snapshotFlow { pagePaletteState.palette }
                            .distinctUntilChanged()
                            .collectLatest { palette ->
                                pageStartColor.animateTo(palette.getColorFromPalette())
                            }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .clickable(
                                enabled = pageHasCanvas,
                                onClick = {
                                    if (state.mainScrollState.value == 0) {
                                        actions.onToggleControls()
                                    }
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ),
                    ) {
                        if (!isCurrentArtworkPage && pageTrack != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                pageStartColor.value,
                                                Color.Black,
                                            ),
                                            start = state.gradientOffset.start,
                                            end = state.gradientOffset.end,
                                        ),
                                    ),
                            )
                        }

                        if (pageHasCanvas) {
                            Crossfade(targetState = state.screenData.canvasData?.isVideo) { isVideo ->
                                if (isVideo == true) {
                                    state.screenData.canvasData?.url?.let { url ->
                                        MediaPlayerView(
                                            url = url,
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .then(
                                                    if (getPlatform() == Platform.Desktop) {
                                                        Modifier
                                                    } else {
                                                        Modifier
                                                            .wrapContentWidth(unbounded = true, align = Alignment.CenterHorizontally)
                                                            .align(Alignment.Center)
                                                    },
                                                ),
                                        )
                                    }
                                } else if (isVideo == false) {
                                    AsyncImage(
                                        model = ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(state.screenData.canvasData?.url)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(state.screenData.canvasData?.url)
                                            .crossfade(550)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Crossfade(
                                targetState = state.showControlLayout,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .align(Alignment.BottomCenter),
                            ) { focused ->
                                if (focused) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                smoothScrimBrush(
                                                    from = overlay,
                                                    to = Color.Black,
                                                    startFraction = 0.2f,
                                                ),
                                            ),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                smoothScrimBrush(
                                                    from = Color.Black.copy(alpha = 0f),
                                                    to = Color.Black,
                                                    startFraction = 0.92f,
                                                    endFraction = 0.97f,
                                                ),
                                            ),
                                    )
                                }
                            }
                        }

                        // Artwork Frame
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.height(topAppBarHeightDp.dp))
                            Spacer(
                                modifier = Modifier
                                    .animateContentSize()
                                    .height(middleLayoutPaddingDp.dp)
                                    .fillMaxWidth(),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 30.dp)
                                    .alpha(if (pageHasCanvas) 0f else 1f)
                                    .aspectRatio(1f),
                            ) {
                                if (isCurrentArtworkPage) {
                                    var artworkUrl by remember(state.screenData.thumbnailURL) {
                                        mutableStateOf(state.screenData.thumbnailURL)
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(
                                                elevation = 18.dp,
                                                shape = RoundedCornerShape(26.dp),
                                                spotColor = state.spotShadowColor.copy(alpha = 0.5f),
                                                ambientColor = Color.Black.copy(alpha = 0.6f),
                                            )
                                            .clip(RoundedCornerShape(26.dp))
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(26.dp),
                                            ),
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(artworkUrl)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(artworkUrl + "BIGGER")
                                                .crossfade(550)
                                                .build(),
                                            contentDescription = "",
                                            onSuccess = {
                                                actions.onArtworkBitmap(it.result.image.toImageBitmap())
                                            },
                                            onError = {
                                                val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                                                if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                                            },
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberHolderPainter(),
                                            error = rememberHolderPainter(),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(26.dp))
                                                .alpha(if (!state.screenData.isVideo || !state.shouldShowVideo) 1f else 0f),
                                        )
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = state.screenData.isVideo && state.shouldShowVideo,
                                        modifier = Modifier.align(Alignment.Center),
                                    ) {
                                        var internalShowSubtitle by rememberSaveable { mutableStateOf(true) }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(Color.Black),
                                        ) {
                                            Box(Modifier.fillMaxSize()) {
                                                MediaPlayerViewWithSubtitle(
                                                    playerName = MAIN_PLAYER,
                                                    modifier = Modifier.align(Alignment.Center),
                                                    shouldShowSubtitle = internalShowSubtitle,
                                                    shouldPip = false,
                                                    shouldScaleDownSubtitle = true,
                                                    timelineState = state.timelineState,
                                                    lyricsData = state.screenData.lyricsData?.lyrics,
                                                    translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                                                    isInPipMode = state.isInPipMode,
                                                    mainTextStyle = typo().bodyLarge,
                                                    translatedTextStyle = typo().bodyMedium,
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable(
                                                        onClick = { showHideFullscreenOverlay = !showHideFullscreenOverlay },
                                                        indication = null,
                                                        interactionSource = remember { MutableInteractionSource() },
                                                    ),
                                            ) {
                                                Crossfade(targetState = showHideFullscreenOverlay) {
                                                    if (it) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(
                                                                    smoothScrimBrush(
                                                                        from = blackMoreOverlay,
                                                                        to = overlay.copy(alpha = 0f),
                                                                        startFraction = 0.03f,
                                                                        endFraction = 0.8f,
                                                                    ),
                                                                ),
                                                        ) {
                                                            IconButton(
                                                                onClick = { actions.onEnterFullscreenVideo() },
                                                                Modifier.align(Alignment.TopEnd),
                                                            ) {
                                                                Icon(
                                                                    imageVector = SimpIcons.Fullscreen,
                                                                    contentDescription = "",
                                                                    tint = Color.White,
                                                                )
                                                            }
                                                            Row(
                                                                Modifier
                                                                    .align(Alignment.Center)
                                                                    .fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                            ) {
                                                                FilledTonalIconButton(
                                                                    colors = IconButtonDefaults.iconButtonColors().copy(
                                                                        containerColor = Color.Transparent,
                                                                    ),
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .aspectRatio(1f)
                                                                        .clip(CircleShape),
                                                                    onClick = { actions.onUIEvent(UIEvent.Backward) },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = SimpIcons.Replay5,
                                                                        tint = Color.White,
                                                                        contentDescription = "",
                                                                        modifier = Modifier
                                                                            .size(36.dp)
                                                                            .alpha(0.8f),
                                                                    )
                                                                }
                                                                FilledTonalIconButton(
                                                                    colors = IconButtonDefaults.iconButtonColors().copy(
                                                                        containerColor = Color.Transparent,
                                                                    ),
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .aspectRatio(1f)
                                                                        .clip(CircleShape),
                                                                    onClick = { actions.onUIEvent(UIEvent.Forward) },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = SimpIcons.Forward5,
                                                                        tint = Color.White,
                                                                        contentDescription = "",
                                                                        modifier = Modifier
                                                                            .size(36.dp)
                                                                            .alpha(0.8f),
                                                                    )
                                                                }
                                                            }
                                                            if (state.screenData.lyricsData != null) {
                                                                IconButton(
                                                                    onClick = { internalShowSubtitle = !internalShowSubtitle },
                                                                    Modifier.align(Alignment.BottomEnd),
                                                                ) {
                                                                    Icon(
                                                                        imageVector = if (internalShowSubtitle) SimpIcons.SubtitlesOff else SimpIcons.Subtitles,
                                                                        contentDescription = "",
                                                                        tint = Color.White,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (pageTrack != null) {
                                    val staticThumb = pageTrack.thumbnails
                                        ?.maxByOrNull { it.width * it.height }
                                        ?.url
                                    val palettePageScope = rememberCoroutineScope()
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(
                                                elevation = 16.dp,
                                                shape = RoundedCornerShape(26.dp),
                                                spotColor = Color.Black.copy(alpha = 0.5f),
                                            )
                                            .clip(RoundedCornerShape(26.dp))
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(26.dp),
                                            ),
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(staticThumb)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(staticThumb)
                                                .crossfade(300)
                                                .build(),
                                            contentDescription = pageTrack.title,
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberHolderPainter(),
                                            error = rememberHolderPainter(),
                                            onSuccess = { stateRes ->
                                                palettePageScope.launch {
                                                    pagePaletteState.generate(stateRes.result.image.toImageBitmap())
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(26.dp)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top Liquid Glass Capsule Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() + 4.dp })
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned {
                            topAppBarHeightDp = with(localDensity) { it.size.height.toDp().value.toInt() + 10 }
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 14.dp,
                                shape = RoundedCornerShape(28.dp),
                                spotColor = Color.Black.copy(alpha = 0.50f),
                                ambientColor = Color.Black.copy(alpha = 0.30f),
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                                blurEnabled = true
                            }
                            .background(Color(16, 16, 20).copy(alpha = 0.50f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(28.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(
                                    width = 0.8.dp,
                                    color = Color.White.copy(alpha = 0.20f),
                                    shape = CircleShape,
                                )
                                .clickable { actions.onDismiss() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = state.dismissIcon,
                                contentDescription = "",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(Res.string.now_playing_upper),
                                style = typo().labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.70f),
                            )
                            Text(
                                text = state.screenData.playlistName,
                                style = typo().labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    )
                                    .focusable(),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(
                                    width = 0.8.dp,
                                    color = Color.White.copy(alpha = 0.20f),
                                    shape = CircleShape,
                                )
                                .clickable { actions.onShowMoreSheet() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = SimpIcons.MoreVert,
                                contentDescription = "",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Column {
                    Spacer(modifier = Modifier.height(topAppBarHeightDp.dp))
                    Box {
                        Column(Modifier.fillMaxWidth()) {
                            Spacer(
                                modifier = Modifier
                                    .animateContentSize()
                                    .height(middleLayoutPaddingDp.dp)
                                    .fillMaxWidth(),
                            )

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 30.dp)
                                    .onGloballyPositioned { coords ->
                                        middleLayoutHeightDp = with(localDensity) { coords.size.height.toDp().value.toInt() }
                                    }
                                    .aspectRatio(1f),
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .animateContentSize()
                                    .height(middleLayoutPaddingDp.dp)
                                    .fillMaxWidth(),
                            ) {
                                val inlineLyrics = state.screenData.lyricsData?.lyrics
                                val hasSyncedLyrics = inlineLyrics != null &&
                                    inlineLyrics.syncType != null &&
                                    inlineLyrics.syncType != "UNSYNCED" &&
                                    inlineLyrics.lines != null

                                val currentLyricLineText = if (!hasSyncedLyrics ||
                                    state.screenData.canvasData != null ||
                                    state.currentLyricLineIndex < 0
                                ) {
                                    ""
                                } else {
                                    inlineLyrics
                                        ?.lines
                                        ?.getOrNull(state.currentLyricLineIndex)
                                        ?.words
                                        ?.stripRichSyncTimestamps()
                                        .orEmpty()
                                }
                                Crossfade(
                                    targetState = currentLyricLineText,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "inlineLyricLine",
                                ) { lineText ->
                                    Text(
                                        text = lineText,
                                        style = typo().labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            )
                                            .focusable(),
                                    )
                                }
                            }

                            // Controls Dock with Guaranteed Lift above Phone Navigation Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = navBarBottomPaddingDp),
                            ) {
                                Column(
                                    Modifier
                                        .alpha(state.controlLayoutAlpha)
                                        .padding(horizontal = 14.dp)
                                        .shadow(
                                            elevation = 18.dp,
                                            shape = RoundedCornerShape(32.dp),
                                            spotColor = Color.Black.copy(alpha = 0.6f),
                                            ambientColor = Color.Black.copy(alpha = 0.4f),
                                        )
                                        .clip(RoundedCornerShape(32.dp))
                                        .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                                            blurEnabled = true
                                        }
                                        .background(Color(14, 14, 18).copy(alpha = 0.52f))
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(32.dp),
                                        )
                                        .padding(vertical = 12.dp)
                                        .onGloballyPositioned {
                                            infoLayoutHeightDp = with(localDensity) { it.size.height.toDp().value.toInt() }
                                        },
                                ) {
                                    NowPlayingTrackInfoRow(
                                        state = state,
                                        actions = actions,
                                    )
                                    if (getPlatform() == Platform.Android) {
                                        Box(
                                            Modifier
                                                .padding(top = 8.dp)
                                                .padding(horizontal = 18.dp)
                                                .isElementVisible {
                                                    actions.onToolbarVisibilityChange(!it && state.isExpanded && state.mainScrollState.value > 0)
                                                },
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(24.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Crossfade(state.timelineState.loading) {
                                                    if (it) {
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                            LinearProgressIndicator(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(4.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                color = Color.Gray,
                                                                trackColor = Color.DarkGray,
                                                                strokeCap = StrokeCap.Round,
                                                            )
                                                        }
                                                    } else {
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                            LinearProgressIndicator(
                                                                progress = { state.timelineState.bufferedPercent.toFloat() / 100 },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(4.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                color = Color.White.copy(alpha = 0.25f),
                                                                trackColor = Color.White.copy(alpha = 0.08f),
                                                                strokeCap = StrokeCap.Round,
                                                                drawStopIndicator = {},
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                Slider(
                                                    value = state.sliderValue / 100f,
                                                    onValueChangeFinished = {
                                                        actions.onSliderChangeFinished()
                                                    },
                                                    onValueChange = {
                                                        actions.onSliderChange(it * 100f)
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 3.dp)
                                                        .align(Alignment.TopCenter),
                                                    track = { sliderState ->
                                                        SliderDefaults.Track(
                                                            modifier = Modifier.height(5.dp),
                                                            enabled = true,
                                                            sliderState = sliderState,
                                                            colors = SliderDefaults.colors().copy(
                                                                thumbColor = state.sliderTrackColor,
                                                                activeTrackColor = state.sliderTrackColor,
                                                                inactiveTrackColor = Color.Transparent,
                                                            ),
                                                            thumbTrackGapSize = 0.dp,
                                                            drawTick = { _, _ -> },
                                                            drawStopIndicator = null,
                                                        )
                                                    },
                                                    thumb = {
                                                        SliderDefaults.Thumb(
                                                            modifier = Modifier
                                                                .height(18.dp)
                                                                .width(8.dp)
                                                                .padding(vertical = 4.dp),
                                                            thumbSize = DpSize(8.dp, 8.dp),
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            colors = SliderDefaults.colors().copy(
                                                                thumbColor = state.sliderTrackColor,
                                                                activeTrackColor = state.sliderTrackColor,
                                                                inactiveTrackColor = Color.Transparent,
                                                            ),
                                                            enabled = true,
                                                        )
                                                    },
                                                )
                                            }
                                        }

                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp),
                                        ) {
                                            Text(
                                                text = formatDuration((state.timelineState.total * (state.sliderValue / 100f)).roundToLong()),
                                                style = typo().bodySmall,
                                                color = Color.White.copy(alpha = 0.65f),
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Left,
                                            )
                                            val sweepTransition = rememberInfiniteTransition(label = "nowPlayingCrossfadeSweep")
                                            val crossfadeSweep by sweepTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(3200, easing = LinearEasing),
                                                    repeatMode = RepeatMode.Restart,
                                                ),
                                                label = "nowPlayingSweepHead",
                                            )
                                            AnimatedVisibility(
                                                enter = fadeIn(),
                                                exit = fadeOut(),
                                                visible = state.timelineState.isCrossfading,
                                            ) {
                                                val shimmerSpan = 140f
                                                val shimmerHead = crossfadeSweep * (shimmerSpan * 3f) - shimmerSpan
                                                val labelColor = typo().bodySmall.color
                                                Text(
                                                    text = stringResource(Res.string.crossfading),
                                                    style = typo().bodySmall.copy(
                                                        brush = Brush.horizontalGradient(
                                                            0f to labelColor.copy(alpha = 0.45f),
                                                            0.5f to Color.White,
                                                            1f to labelColor.copy(alpha = 0.45f),
                                                            startX = shimmerHead,
                                                            endX = shimmerHead + shimmerSpan,
                                                            tileMode = TileMode.Clamp,
                                                        ),
                                                    ),
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                            Text(
                                                text = formatDuration(state.timelineState.total),
                                                style = typo().bodySmall,
                                                color = Color.White.copy(alpha = 0.65f),
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Right,
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        PlayerControlLayout(state.controllerState) {
                                            actions.onUIEvent(it)
                                        }
                                    } else {
                                        Spacer(Modifier.height(16.dp))
                                    }

                                    Row(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f, fill = false),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.10f))
                                                    .clickable { actions.onShowInfo() },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.Info,
                                                    tint = Color.White,
                                                    contentDescription = "",
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }

                                            PlatformCastButton(
                                                modifier = Modifier.size(24.dp),
                                                tint = if (state.castState.isRemote) Color.Cyan else Color.White,
                                            )
                                            AnimatedVisibility(visible = state.castState.isRemote) {
                                                Text(
                                                    text = stringResource(
                                                        Res.string.playing_on_device,
                                                        state.castState.deviceName ?: "Cast",
                                                    ),
                                                    style = typo().bodySmall,
                                                    color = Color.Cyan,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.10f))
                                                    .clickable { actions.onShowAddToPlaylist() },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.PlaylistAdd,
                                                    tint = Color.White,
                                                    contentDescription = "Add to Playlist",
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.10f))
                                                    .clickable { actions.onShowQueue() },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.QueueMusic,
                                                    tint = Color.White,
                                                    contentDescription = "",
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                this@Column.AnimatedVisibility(
                                    visible = !state.showControlLayout,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(infoLayoutHeightDp.dp)
                                            .fillMaxWidth()
                                            .clickable(
                                                onClick = {
                                                    if (state.mainScrollState.value == 0) {
                                                        actions.onToggleControls()
                                                    }
                                                },
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                            ),
                                        contentAlignment = Alignment.BottomStart,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    smoothScrimBrush(
                                                        from = Color.Black.copy(alpha = 0f),
                                                        to = Color.Black.copy(alpha = 0.85f),
                                                    ),
                                                ),
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateContentSize(),
                                        ) {
                                            this@Column.AnimatedVisibility(
                                                visible = state.currentLyricLineIndex > -1,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically(),
                                            ) {
                                                val lineText = state.screenData.lyricsData
                                                    ?.lyrics
                                                    ?.lines
                                                    ?.getOrNull(state.currentLyricLineIndex)
                                                    ?.words
                                                    ?.stripRichSyncTimestamps()
                                                if (!lineText.isNullOrBlank()) {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        Text(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 20.dp)
                                                                .padding(bottom = 4.dp)
                                                                .basicMarquee(
                                                                    iterations = Int.MAX_VALUE,
                                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                                )
                                                                .focusable(),
                                                            text = lineText,
                                                            style = typo().bodyMedium,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                        )
                                                        val translatedLineText = state.screenData.lyricsData
                                                            ?.translatedLyrics
                                                            ?.first
                                                            ?.lines
                                                            ?.getOrNull(state.currentLyricLineIndex)
                                                            ?.words
                                                            ?.stripRichSyncTimestamps()
                                                        if (!translatedLineText.isNullOrBlank()) {
                                                            Text(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 20.dp)
                                                                    .padding(bottom = 8.dp)
                                                                    .basicMarquee(
                                                                        iterations = Int.MAX_VALUE,
                                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                                    )
                                                                    .focusable(),
                                                                text = translatedLineText,
                                                                style = typo().bodyMedium,
                                                                color = Color.Yellow,
                                                                maxLines = 1,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            NowPlayingTrackInfoRow(
                                                state = state,
                                                actions = actions,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Content Section with Frosted Glass Panels
                    Column(Modifier.padding(horizontal = 14.dp)) {
                        AnimatedVisibility(
                            visible = state.screenData.lyricsData != null,
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        spotColor = Color.Black.copy(alpha = 0.45f),
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(Color(18, 18, 22).copy(alpha = 0.65f))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.25f),
                                                Color.White.copy(alpha = 0.05f),
                                            ),
                                        ),
                                        shape = RoundedCornerShape(26.dp),
                                    )
                                    .padding(16.dp),
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(Res.string.lyrics),
                                            style = typo().labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
                                        )
                                        if (state.screenData.lyricsData?.translatedLyrics?.second == LyricsProvider.AI) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            AIBadge()
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (state.screenData.lyricsData.canVote()) {
                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                IconButton(onClick = { actions.onShowVoteDialog() }) {
                                                    Icon(
                                                        imageVector = SimpIcons.ThumbsUpDown,
                                                        contentDescription = stringResource(Res.string.rate_lyrics),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                            IconButton(onClick = { showShareLyricsSheet = true }) {
                                                Icon(
                                                    imageVector = SimpIcons.Share,
                                                    contentDescription = stringResource(Res.string.share_lyrics),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                            TextButton(
                                                onClick = { actions.onShowFullscreenLyrics() },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier
                                                    .height(20.dp)
                                                    .wrapContentWidth(),
                                            ) {
                                                Text(text = stringResource(Res.string.show), color = Color.White)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp),
                                    ) {
                                        state.screenData.lyricsData?.let {
                                            LyricsView(
                                                lyricsData = it,
                                                timeLine = state.timelineFlow,
                                                onLineClick = { f ->
                                                    actions.onUIEvent(UIEvent.UpdateProgress(f))
                                                },
                                            )
                                        }
                                    }

                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = when (state.screenData.lyricsData?.lyrics?.syncType) {
                                                "LINE_SYNCED" -> stringResource(Res.string.line_synced)
                                                "RICH_SYNCED" -> stringResource(Res.string.rich_synced)
                                                else -> stringResource(Res.string.unsynced)
                                            },
                                            style = typo().bodySmall,
                                            color = Color.White.copy(alpha = 0.6f),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp),
                                        )
                                        Text(
                                            text = when (state.screenData.lyricsData?.lyricsProvider) {
                                                LyricsProvider.SIMPMUSIC -> stringResource(Res.string.lyrics_provider_simpmusic)
                                                LyricsProvider.LRCLIB -> stringResource(Res.string.lyrics_provider_lrc)
                                                LyricsProvider.YOUTUBE -> stringResource(Res.string.lyrics_provider_youtube)
                                                LyricsProvider.SPOTIFY -> stringResource(Res.string.spotify_lyrics_provider)
                                                LyricsProvider.OFFLINE -> stringResource(Res.string.offline_mode)
                                                LyricsProvider.BETTER_LYRICS -> stringResource(Res.string.lyrics_provider_betterlyrics)
                                                else -> ""
                                            },
                                            style = typo().bodySmall,
                                            color = Color.White.copy(alpha = 0.5f),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedVisibility(visible = state.screenData.songInfoData != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        spotColor = Color.Black.copy(alpha = 0.45f),
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(Color(18, 18, 22).copy(alpha = 0.65f))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.25f),
                                                Color.White.copy(alpha = 0.05f),
                                            ),
                                        ),
                                        shape = RoundedCornerShape(26.dp),
                                    )
                                    .clickable { actions.onNavigateToArtist() },
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp),
                                    ) {
                                        val thumb = state.screenData.songInfoData?.authorThumbnail
                                        AsyncImage(
                                            model = ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(thumb)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(thumb)
                                                .crossfade(550)
                                                .build(),
                                            placeholder = rememberHolderPainter(isVideo = true),
                                            error = rememberHolderPainter(isVideo = true),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    smoothScrimBrush(
                                                        from = Color.Black.copy(alpha = 0.6f),
                                                        to = Color.Black.copy(alpha = 0.0f),
                                                        endFraction = 0.4f,
                                                    ),
                                                ),
                                        )
                                        Text(
                                            text = stringResource(Res.string.artists),
                                            style = typo().labelMedium,
                                            color = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(15.dp),
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 15.dp, vertical = 12.dp),
                                    ) {
                                        Text(
                                            text = state.screenData.songInfoData?.author ?: "",
                                            style = typo().titleMedium,
                                            color = Color.White,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.screenData.songInfoData?.subscribers ?: "",
                                            style = typo().bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedVisibility(visible = state.screenData.songInfoData != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        spotColor = Color.Black.copy(alpha = 0.45f),
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(Color(18, 18, 22).copy(alpha = 0.65f))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.25f),
                                                Color.White.copy(alpha = 0.05f),
                                            ),
                                        ),
                                        shape = RoundedCornerShape(26.dp),
                                    )
                                    .padding(16.dp),
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth(),
                                ) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = stringResource(Res.string.published_at, state.screenData.songInfoData?.uploadDate ?: ""),
                                        style = typo().labelSmall,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(
                                            Res.string.view_count,
                                            "%,d".format(state.screenData.songInfoData?.viewCount),
                                        ),
                                        style = typo().labelMedium,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(
                                            Res.string.like_and_dislike,
                                            state.screenData.songInfoData?.like ?: 0,
                                            state.screenData.songInfoData?.dislike ?: 0,
                                        ),
                                        style = typo().bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(Res.string.description),
                                        style = typo().labelSmall,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DescriptionView(
                                        text = state.screenData.songInfoData?.description ?: "",
                                        onTimeClicked = { raw ->
                                            val timestamp = parseTimestampToMilliseconds(raw)
                                            if (timestamp != 0.0 && timestamp < state.timelineState.total) {
                                                actions.onUIEvent(
                                                    UIEvent.UpdateProgress(
                                                        ((timestamp * 100) / state.timelineState.total).toFloat(),
                                                    ),
                                                )
                                            }
                                        },
                                        onURLClicked = { url -> uriHandler.openUri(url) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Spacer(
                            modifier = Modifier.height(
                                with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() },
                            ),
                        )
                    }
                }
            }
        }

        // Mini Sticky Floating Glass Toolbar
        AnimatedVisibility(
            visible = state.shouldShowToolbar && state.isExpanded,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() + 4.dp })
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = Color.Black.copy(alpha = 0.55f),
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    }
                    .background(Color(14, 14, 18).copy(alpha = 0.65f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(26.dp),
                    ),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 14.dp)
                            .fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.weight(1F)) {
                            Column(Modifier.wrapContentHeight()) {
                                Text(
                                    text = state.screenData.nowPlayingTitle,
                                    style = typo().bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        )
                                        .focusable(),
                                )
                                LazyRow(verticalAlignment = Alignment.CenterVertically) {
                                    item {
                                        AnimatedVisibility(visible = state.screenData.isExplicit) {
                                            ExplicitBadge(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(end = 4.dp)
                                                    .weight(1f),
                                            )
                                        }
                                    }
                                    item(key = state.screenData.artistName) {
                                        Text(
                                            text = state.screenData.artistName,
                                            style = typo().bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                )
                                                .focusable(),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        HeartCheckBox(checked = state.controllerState.isLiked, size = 28) {
                            actions.onUIEvent(UIEvent.ToggleLike)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Crossfade(targetState = state.timelineState.loading, label = "") {
                            if (it) {
                                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.LightGray,
                                        strokeWidth = 2.5.dp,
                                    )
                                }
                            } else {
                                PlayPauseButton(isPlaying = state.controllerState.isPlaying, modifier = Modifier.size(40.dp)) {
                                    actions.onUIEvent(UIEvent.PlayPause)
                                }
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { state.timelineState.current.toFloat() / state.timelineState.total },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }

    state.screenData.lyricsData?.let { lyricsData ->
        if (showShareLyricsSheet) {
            ShareLyricsSheet(
                lines = lyricsData.toShareLyricsLines(),
                songTitle = state.screenData.nowPlayingTitle,
                artistName = state.screenData.artistName,
                artwork = state.screenData.bitmap,
                seedColor = state.startColor.value,
                initialLineIndex = state.currentLyricLineIndex,
                onDismiss = { showShareLyricsSheet = false },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingTrackInfoRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(state.screenData.canvasData != null) {
            AsyncImage(
                model = ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(state.screenData.thumbnailURL)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(state.screenData.thumbnailURL + "BIGGER")
                    .crossfade(true)
                    .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .heightIn(0.dp, 50.dp)
                    .width(50.dp)
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.CenterVertically),
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typo().titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately,
                    )
                    .focusable(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(state.screenData.isExplicit) {
                    AnimatedVisibility(visible = state.screenData.isExplicit) {
                        ExplicitBadge(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                                .weight(1f),
                        )
                    }
                }
                item(state.screenData.artistName) {
                    Text(
                        text = state.screenData.artistName,
                        style = typo().bodyMedium,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            )
                            .focusable()
                            .clickable {
                                actions.onNavigateToArtist()
                            },
                    )
                }
            }
        }
        if (state.isUserLoggedIn) {
            Spacer(modifier = Modifier.size(12.dp))
            Crossfade(targetState = state.likeStatus) {
                if (it) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { actions.onAddToYouTubeLiked() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.CheckCircle,
                            tint = Color.White,
                            contentDescription = "",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { actions.onAddToYouTubeLiked() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.AddCircleOutline,
                            tint = Color.White,
                            contentDescription = "",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
        HeartCheckBox(checked = state.controllerState.isLiked, size = 30) {
            actions.onUIEvent(UIEvent.ToggleLike)
        }
    }
}