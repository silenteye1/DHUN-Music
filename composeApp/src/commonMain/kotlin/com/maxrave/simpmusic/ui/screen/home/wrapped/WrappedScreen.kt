package com.maxrave.simpmusic.ui.screen.home.wrapped

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.kyant.backdrop.highlight.Highlight
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.maxrave.common.Config
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.saveImageToDevice
import com.maxrave.simpmusic.expect.shareImage
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.rememberSaveImagePermission
import com.maxrave.simpmusic.expect.ui.toPngByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.capture.CaptureController
import com.maxrave.simpmusic.ui.component.capture.capturable
import com.maxrave.simpmusic.ui.component.capture.rememberCaptureController
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.VolumeOff
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedBiggestDayCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedClockCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedDecadesCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedEyebrow
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedMinutesCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedOpeningCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedShareCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTopAlbumsCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTopArtistsCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTopTracksCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTypeCard
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import com.maxrave.simpmusic.viewModel.WrappedCard
import com.maxrave.simpmusic.viewModel.WrappedUiState
import com.maxrave.simpmusic.viewModel.WrappedViewModel
import com.maxrave.simpmusic.viewModel.WrappedYear
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import kotlin.random.Random
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.share_lyrics_permission_denied
import simpmusic.composeapp.generated.resources.share_lyrics_save_failed
import simpmusic.composeapp.generated.resources.share_lyrics_saved
import simpmusic.composeapp.generated.resources.share_lyrics_saved_desktop
import simpmusic.composeapp.generated.resources.share_lyrics_share_failed
import simpmusic.composeapp.generated.resources.wrapped_share
import simpmusic.composeapp.generated.resources.wrapped_tap_to_begin
import simpmusic.composeapp.generated.resources.wrapped_tap_to_continue
import simpmusic.composeapp.generated.resources.wrapped_year

@Composable
fun WrappedScreen(
    navController: NavController,
    hideNavBar: () -> Unit,
    showNavBar: () -> Unit,
    wrappedViewModel: WrappedViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val state by wrappedViewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        hideNavBar()
        onDispose { showNavBar() }
    }

    WrappedTheme(
        artworkUrl = (state as? WrappedUiState.Ready)?.wrapped?.topTracks?.firstOrNull()?.song?.thumbnails,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            when (val current = state) {
                is WrappedUiState.Loading -> CenterLoadingBox(modifier = Modifier.fillMaxSize())

                is WrappedUiState.NotEnoughData ->
                    WrappedNotEnoughDataScreen(
                        state = current,
                        onBack = { navController.navigateUp() },
                    )

                is WrappedUiState.Ready ->
                    WrappedReel(
                        wrapped = current.wrapped,
                        sharedViewModel = sharedViewModel,
                        onClose = { navController.navigateUp() },
                    )
            }
        }
    }
}

@Composable
private fun WrappedTheme(
    artworkUrl: String?,
    content: @Composable () -> Unit,
) {
    val httpClient = remember { HttpClient(CIO) }
    val networkLoader = rememberNetworkLoader(httpClient)
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = seed,
            defaultOnColor = seed,
            loader = networkLoader,
        )
    LaunchedEffect(artworkUrl) {
        artworkUrl?.takeIf { it.isNotBlank() }?.let { dominantColorState.updateFrom(Url(it)) }
    }
    val animatedSeed by animateColorAsState(
        targetValue = dominantColorState.color,
        animationSpec = tween(durationMillis = 800),
        label = "wrappedSeed",
    )
    MaterialExpressiveTheme(
        colorScheme =
            rememberDynamicColorScheme(
                seedColor = animatedSeed,
                isDark = true,
                isAmoled = false,
                style = PaletteStyle.Vibrant,
            ),
        content = content,
    )
}

@Composable
private fun WrappedReel(
    wrapped: WrappedYear,
    sharedViewModel: SharedViewModel,
    onClose: () -> Unit,
) {
    val cards = remember(wrapped) { wrapped.cards }
    val pagerState = rememberPagerState(pageCount = { cards.size })
    val scope = rememberCoroutineScope()

    val page = pagerState.currentPage
    val isShareCard = cards[page] == WrappedCard.SHARE

    // Background playback of user's #1 Top Track with sound toggle
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    var isSoundMuted by rememberSaveable { mutableStateOf(false) }
    var hasStartedTopTrack by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(wrapped.year) {
        val topSong = wrapped.topTracks.firstOrNull()?.song
        if (!hasStartedTopTrack && topSong != null && sharedViewModel.nowPlayingState.value?.songEntity?.videoId != topSong.videoId) {
            hasStartedTopTrack = true
            val trackList = ArrayList(wrapped.topTracks.map { it.song.toTrack() })
            sharedViewModel.setQueueData(
                QueueData.Data(
                    listTracks = trackList,
                    firstPlayedTrack = topSong.toTrack(),
                    playlistId = "WRAPPED_${wrapped.year}",
                    playlistName = "Dhun Wrapped ${wrapped.year}",
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            sharedViewModel.loadMediaItem(topSong, type = Config.SONG_CLICK, index = 0)
        }
    }

    var progress by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val captureController = rememberCaptureController()
    val share = rememberWrappedShare(wrapped, captureController) { busy = it }

    val backdrop = rememberBackdrop(MaterialTheme.colorScheme.background)

    fun goTo(target: Int) {
        when {
            target < 0 -> Unit
            target >= cards.size -> onClose()
            else -> scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    LaunchedEffect(page, cards) {
        progress = 0f
        val holdMs = if (cards[page] == WrappedCard.SHARE) WrappedTokens.SHARE_CARD_DURATION_MS else WrappedTokens.CARD_DURATION_MS
        var elapsed = 0L
        var lastFrame = 0L
        while (elapsed < holdMs) {
            withFrameMillis { frame ->
                val delta = if (lastFrame == 0L) 0L else frame - lastFrame
                lastFrame = frame
                if (!pressed && !busy && !pagerState.isScrollInProgress) elapsed += delta
            }
            progress = (elapsed.toFloat() / holdMs).coerceIn(0f, 1f)
        }
        goTo(page + 1)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(cards) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onLongPress = { },
                        onTap = { offset ->
                            val from = pagerState.targetPage
                            goTo(if (offset.x > size.width / 2f) from + 1 else from - 1)
                        },
                    )
                },
    ) {
        Box(modifier = Modifier.matchParentSize().layerBackdrop(backdrop)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(top = WrappedTokens.HeaderHeight),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { cards[it].name },
                ) { index ->
                    WrappedCardSlot(
                        card = cards[index],
                        wrapped = wrapped,
                        captureController = captureController,
                        onSave = share.onSave,
                        onShare = share.onShare,
                    )
                }
            }

            val ground = MaterialTheme.colorScheme.background
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(SCRIM_HEIGHT)
                        .background(
                            Brush.verticalGradient(
                                0f to ground.copy(alpha = 0.62f),
                                0.52f to ground.copy(alpha = 0.28f),
                                1f to ground.copy(alpha = 0f),
                            ),
                        ),
            )
        }

        WrappedHeader(
            year = wrapped.year,
            cardCount = cards.size,
            currentCard = page,
            progress = { progress },
            backdrop = backdrop,
            isSoundMuted = isSoundMuted || !controllerState.isPlaying,
            onToggleSound = {
                isSoundMuted = !isSoundMuted
                sharedViewModel.onUIEvent(UIEvent.PlayPause)
            },
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        WrappedFooter(
            isFirstCard = page == 0,
            showActions = !isShareCard,
            backdrop = backdrop,
            onShare = share.onShare,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun WrappedCardSlot(
    card: WrappedCard,
    wrapped: WrappedYear,
    captureController: CaptureController,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val isShareCard = card == WrappedCard.SHARE
    val capture = Modifier.capturable(captureController)
    val slot =
        Modifier
            .fillMaxSize()
            .padding(bottom = if (isShareCard) 0.dp else WrappedTokens.FooterHeight)
            .background(MaterialTheme.colorScheme.background)
            .then(if (isShareCard) Modifier else capture)

    when (card) {
        WrappedCard.OPENING -> WrappedOpeningCard(wrapped = wrapped, modifier = slot)
        WrappedCard.MINUTES -> WrappedMinutesCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TOP_TRACKS -> WrappedTopTracksCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TOP_ARTISTS -> WrappedTopArtistsCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TOP_ALBUMS -> WrappedTopAlbumsCard(wrapped = wrapped, modifier = slot)
        WrappedCard.CLOCK -> WrappedClockCard(wrapped = wrapped, modifier = slot)
        WrappedCard.BIGGEST_DAY -> WrappedBiggestDayCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TYPE -> WrappedTypeCard(wrapped = wrapped, modifier = slot)
        WrappedCard.DECADES -> WrappedDecadesCard(wrapped = wrapped, modifier = slot)
        WrappedCard.SHARE ->
            WrappedShareCard(
                wrapped = wrapped,
                onSave = onSave,
                onShare = onShare,
                modifier = slot,
                posterModifier = capture,
            )
    }
}

@Composable
private fun WrappedHeader(
    year: Int,
    cardCount: Int,
    currentCard: Int,
    progress: () -> Float,
    backdrop: PlatformBackdrop,
    isSoundMuted: Boolean,
    onToggleSound: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(WrappedTokens.HeaderHeight),
    ) {
        Spacer(modifier = Modifier.height(SEGMENT_TOP_INSET))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(WrappedTokens.SegmentGap),
        ) {
            repeat(cardCount) { index ->
                LinearProgressIndicator(
                    progress = {
                        when {
                            index < currentCard -> 1f
                            index == currentCard -> progress().coerceIn(0f, 1f)
                            else -> 0f
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SEGMENT_TRACK_ALPHA),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(WrappedTokens.SegmentHeight),
                )
            }
        }
        Spacer(modifier = Modifier.height(SEGMENT_BOTTOM_INSET))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(CLOSE_BUTTON_SIZE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WrappedEyebrow(
                text = stringResource(Res.string.wrapped_year, year.toString()),
                modifier = Modifier.weight(1f),
            )
            LiquidGlassIconButton(
                backdrop = backdrop,
                imageVector = if (isSoundMuted) SimpIcons.VolumeOff else SimpIcons.VolumeUp,
                tint = MaterialTheme.colorScheme.onSurface,
                highlight = Highlight(width = WrappedTokens.GlassRimWidth),
                modifier = Modifier.size(CLOSE_BUTTON_SIZE),
                onClick = onToggleSound,
            )
            Spacer(modifier = Modifier.width(8.dp))
            LiquidGlassIconButton(
                backdrop = backdrop,
                imageVector = SimpIcons.Close,
                tint = MaterialTheme.colorScheme.onSurface,
                highlight = Highlight(width = WrappedTokens.GlassRimWidth),
                modifier = Modifier.size(CLOSE_BUTTON_SIZE),
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun WrappedFooter(
    isFirstCard: Boolean,
    showActions: Boolean,
    backdrop: PlatformBackdrop,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showActions) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .height(WrappedTokens.FooterHeight)
                .padding(start = 24.dp, end = 24.dp, bottom = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                stringResource(
                    if (isFirstCard) Res.string.wrapped_tap_to_begin else Res.string.wrapped_tap_to_continue,
                ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier =
                Modifier
                    .height(SHARE_PILL_HEIGHT)
                    .liquidGlass(backdrop, RoundedCornerShape(SHARE_PILL_HEIGHT / 2))
                    .clip(RoundedCornerShape(SHARE_PILL_HEIGHT / 2))
                    .clickable(onClick = onShare)
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SimpIcons.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.wrapped_share),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

private class WrappedShareActions(
    val onSave: () -> Unit,
    val onShare: () -> Unit,
)

@Composable
private fun rememberWrappedShare(
    wrapped: WrappedYear,
    captureController: CaptureController,
    onBusy: (Boolean) -> Unit,
): WrappedShareActions {
    val scope = rememberCoroutineScope()

    val savedMessage =
        stringResource(
            if (getPlatform() == Platform.Desktop) Res.string.share_lyrics_saved_desktop else Res.string.share_lyrics_saved,
        )
    val saveFailedMessage = stringResource(Res.string.share_lyrics_save_failed)
    val shareFailedMessage = stringResource(Res.string.share_lyrics_share_failed)
    val permissionDeniedMessage = stringResource(Res.string.share_lyrics_permission_denied)
    val chooserTitle = stringResource(Res.string.wrapped_year, wrapped.year.toString())

    val fileName =
        remember(wrapped.year) {
            "Dhun_Wrapped_${wrapped.year}_${Random.nextInt(100_000, 999_999)}.png"
        }

    val savePermission =
        rememberSaveImagePermission { granted ->
            if (!granted) {
                showToast(permissionDeniedMessage, ToastGravity.Bottom)
                onBusy(false)
                return@rememberSaveImagePermission
            }
            scope.launch {
                val bytes = captureController.captureAsync().await().toPngByteArray()
                val ok = bytes != null && saveImageToDevice(bytes, fileName)
                showToast(if (ok) savedMessage else saveFailedMessage, ToastGravity.Bottom)
                onBusy(false)
            }
        }

    return remember(savePermission, fileName, chooserTitle, shareFailedMessage) {
        WrappedShareActions(
            onSave = {
                onBusy(true)
                savePermission.requestIfNeeded()
            },
            onShare = {
                onBusy(true)
                scope.launch {
                    val bytes = captureController.captureAsync().await().toPngByteArray()
                    val ok = bytes != null && shareImage(bytes, fileName, chooserTitle)
                    if (!ok) showToast(shareFailedMessage, ToastGravity.Bottom)
                    onBusy(false)
                }
            },
        )
    }
}

private val SCRIM_HEIGHT = 118.dp
private val SEGMENT_TOP_INSET = 12.dp
private val SEGMENT_BOTTOM_INSET = 9.dp
private val CLOSE_BUTTON_SIZE = 48.dp
private val SHARE_PILL_HEIGHT = 40.dp
private const val SEGMENT_TRACK_ALPHA = 0.24f