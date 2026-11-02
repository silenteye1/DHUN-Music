package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.common.CHART_SUPPORTED_COUNTRY
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItem
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.ItemArtistChart
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.OfflineErrorState
import com.maxrave.simpmusic.ui.component.QuickPicksItem
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.Notifications
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.RecentlySongsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_COMMUTE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_ENERGIZE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_FEEL_GOOD
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_FOCUS
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_PARTY
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_RELAX
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_ROMANCE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_SAD
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_SLEEP
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_WORKOUT
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import java.io.File
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.chart
import simpmusic.composeapp.generated.resources.commute
import simpmusic.composeapp.generated.resources.do_not_show_again
import simpmusic.composeapp.generated.resources.energize
import simpmusic.composeapp.generated.resources.feel_good
import simpmusic.composeapp.generated.resources.focus
import simpmusic.composeapp.generated.resources.go_to_log_in_page
import simpmusic.composeapp.generated.resources.good_afternoon
import simpmusic.composeapp.generated.resources.good_evening
import simpmusic.composeapp.generated.resources.good_morning
import simpmusic.composeapp.generated.resources.good_night
import simpmusic.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import simpmusic.composeapp.generated.resources.let_s_start_with_a_radio
import simpmusic.composeapp.generated.resources.log_in_warning
import simpmusic.composeapp.generated.resources.party
import simpmusic.composeapp.generated.resources.quick_picks
import simpmusic.composeapp.generated.resources.relax
import simpmusic.composeapp.generated.resources.romance
import simpmusic.composeapp.generated.resources.sad
import simpmusic.composeapp.generated.resources.sleep
import simpmusic.composeapp.generated.resources.top_artists
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.welcome_back
import simpmusic.composeapp.generated.resources.what_is_best_choice_today
import simpmusic.composeapp.generated.resources.workout

private val listOfHomeChip =
    listOf(
        Res.string.all,
        Res.string.relax,
        Res.string.sleep,
        Res.string.energize,
        Res.string.sad,
        Res.string.romance,
        Res.string.feel_good,
        Res.string.workout,
        Res.string.party,
        Res.string.commute,
        Res.string.focus,
    )

/**
 * True Full-Screen Smooth Ambient Wallpaper:
 * Smooth gradient mesh fills the canvas evenly without any circular spotlight cutoff.
 */
@Composable
fun DynamicAmbientBackground(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(1200),
        label = "PrimaryAmbient",
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(1200),
        label = "SecondaryAmbient",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.35f),
                        Color(0xFF070B12),
                        animatedSecondary.copy(alpha = 0.28f),
                        Color(0xFF04060A),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            animatedSecondary.copy(alpha = 0.25f),
                            Color.Transparent,
                            animatedPrimary.copy(alpha = 0.20f),
                        ),
                    ),
                ),
        )
    }
}

/**
 * Liquid Glass Shader for HomeScreen:
 * - Highlight.Plain avoids harsh diagonal corner artifacts[cite: 4]
 * - Instant load without GPU buffer readback
 */
@Composable
fun Modifier.homeCleanLiquidGlass(
    backdrop: PlatformBackdrop,
    shape: Shape = RoundedCornerShape(22.dp),
): Modifier {
    if (!LocalLiquidGlassEnabled.current) {
        return this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
    }
    val isDark = LocalIsDarkTheme.current
    return this
        .clip(shape)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = { Highlight.Plain },
            effects = {
                vibrancy()
                colorControls(
                    brightness = 0.05f,
                    contrast = 1f,
                    saturation = 1.4f,
                )
                blur(10.dp.toPx())
                lens(14.dp.toPx(), 28.dp.toPx(), false)
            },
            onDrawSurface = {
                drawRect((if (isDark) Color.Black else Color.White).copy(alpha = 0.16f))
            },
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    dataStoreManager: DataStoreManager = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val customBackgroundUri by dataStoreManager.customBackgroundUri.collectAsStateWithLifecycle(initialValue = null)

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var accountShow by rememberSaveable { mutableStateOf(false) }
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()
    var topAppBarHeightPx by rememberSaveable { mutableIntStateOf(0) }
    val localDensity = LocalDensity.current

    val backdrop = rememberBackdrop(Color.Transparent)

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.getHomeItemList(params)
    }
    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                onRefresh.invoke()
            }
        }
    }
    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.reloadDestinationDone()
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }
    LaunchedEffect(key1 = homeData) {
        accountShow = homeData.find { it.subtitle == accountInfo?.first } == null
    }

    val shouldStartPaginate =
        remember {
            derivedStateOf {
                homeListState != ListState.PAGINATION_EXHAUST &&
                    (
                        scrollState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                        ) >= (scrollState.layoutInfo.totalItemsCount - 1)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {
            viewModel.getContinueHomeItem(continuation)
        }
    }

    if (shouldShowLogInAlert) {
        var doNotShowAgain by rememberSaveable { mutableStateOf(false) }
        AlertDialog(
            title = { Text(stringResource(Res.string.warning)) },
            text = {
                Column {
                    Text(text = stringResource(Res.string.log_in_warning))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { doNotShowAgain = !doNotShowAgain }
                            .fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = doNotShowAgain,
                            onCheckedChange = { doNotShowAgain = it },
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(stringResource(Res.string.do_not_show_again))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                    navController.navigate(LoginDestination)
                }) {
                    Text(stringResource(Res.string.go_to_log_in_page))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            onDismissRequest = { viewModel.doneShowLogInAlert() },
        )
    }

    val headerCapsuleShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508))
    ) {
        // ==================== 1. LIVE BACKDROP SOURCE CANVAS ====================
        // key(customBackgroundUri) guarantees that the entire backdrop canvas is rebuilt instantly when wallpaper path changes
        key(customBackgroundUri) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                val context = LocalPlatformContext.current

                val wallpaperFile = remember(customBackgroundUri) {
                    customBackgroundUri?.takeIf { it.isNotEmpty() }?.let { path ->
                        val f = File(path)
                        if (f.exists()) f else null
                    }
                }

                if (wallpaperFile != null) {
                    val lastModified = remember(wallpaperFile) { wallpaperFile.lastModified() }
                    AsyncImage(
                        model = remember(customBackgroundUri, lastModified) {
                            ImageRequest.Builder(context)
                                .data(wallpaperFile)
                                .memoryCacheKey("${customBackgroundUri}_$lastModified")
                                .diskCacheKey("${customBackgroundUri}_$lastModified")
                                .memoryCachePolicy(CachePolicy.WRITE_ONLY) // Purana cached bitmap bypass karega
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    // DEFAULT WALLPAPER: Smooth Multi-Stop Ambient Flow
                    DynamicAmbientBackground(
                        modifier = Modifier.fillMaxSize(),
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        // ==================== 2. SIBLING SCROLLABLE CONTENT ====================
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {},
        ) {
            Crossfade(targetState = loading, label = "Home Shimmer") { loadingState ->
                if (!loadingState) {
                    if (homeData.isEmpty()) {
                        OfflineErrorState(
                            onRetry = onRefresh,
                            onOpenDownloaded = {
                                navController.navigate(
                                    LibraryDynamicPlaylistDestination(
                                        type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                    ),
                                )
                            },
                        )
                        return@Crossfade
                    }

                    LazyColumn(
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 140.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            Spacer(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .height(
                                        if (topAppBarHeightPx > 0) {
                                            with(localDensity) { topAppBarHeightPx.toDp() + 14.dp }
                                        } else {
                                            165.dp
                                        },
                                    ),
                            )
                        }

                        itemsIndexed(homeData, key = { _, item ->
                            item.hashCode().toString()
                        }) { index, item ->
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp),
                            ) {
                                if (index == 0 && accountInfo != null && accountShow) {
                                    AccountLayout(
                                        accountName = accountInfo?.first ?: "",
                                        url = accountInfo?.second ?: "",
                                        backdrop = backdrop,
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }
                                if (item.title == stringResource(Res.string.quick_picks)) {
                                    AnimatedVisibility(
                                        visible = homeData.find { it.title == stringResource(Res.string.quick_picks) } != null,
                                    ) {
                                        QuickPicks(
                                            homeItem = (homeData.find { it.title == stringResource(Res.string.quick_picks) } ?: return@AnimatedVisibility).let { content ->
                                                content.copy(
                                                    contents = content.contents.mapNotNull { ct ->
                                                        ct?.copy(
                                                            artists = ct.artists?.let { art ->
                                                                if (art.size > 1) art.dropLast(1) else art
                                                            },
                                                        )
                                                    },
                                                )
                                            },
                                            navController = navController,
                                            viewModel = viewModel,
                                            backdrop = backdrop,
                                        )
                                    }
                                } else {
                                    HomeSectionCard(backdrop = backdrop) {
                                        HomeItem(
                                            navController = navController,
                                            data = item,
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            AnimatedVisibility(
                                homeListState == ListState.PAGINATING,
                                enter = expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                CenterLoadingBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                )
                            }
                        }
                        if (homeListState == ListState.PAGINATION_EXHAUST) {
                            items(newRelease, key = { it.hashCode() }) {
                                AnimatedVisibility(visible = newRelease.isNotEmpty()) {
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        HomeSectionCard(backdrop = backdrop) {
                                            HomeItem(navController = navController, data = it)
                                        }
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(visible = moodMomentAndGenre != null) {
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        moodMomentAndGenre?.let {
                                            HomeSectionCard(backdrop = backdrop) {
                                                MoodMomentAndGenre(mood = it, navController = navController)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { EndOfPage() }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                    ) {
                        Spacer(
                            Modifier.height(
                                if (topAppBarHeightPx > 0) {
                                    with(localDensity) { topAppBarHeightPx.toDp() + 14.dp }
                                } else {
                                    165.dp
                                },
                            ),
                        )
                        HomeSectionCard(backdrop = backdrop) {
                            HomeShimmer()
                        }
                    }
                }
            }
        }

        // ==================== 3. FLOATING TOP LIQUID GLASS BAR ====================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .zIndex(100f)
                .onGloballyPositioned { coordinates ->
                    topAppBarHeightPx = coordinates.size.height
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .homeCleanLiquidGlass(
                        backdrop = backdrop,
                        shape = headerCapsuleShape,
                    ),
            ) {
                HomeTopAppBar(navController)

                Row(
                    modifier = Modifier
                        .horizontalScroll(chipRowState)
                        .padding(bottom = 12.dp, start = 14.dp, end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOfHomeChip.forEach { id ->
                        val isSelected = when (params) {
                            HOME_PARAMS_RELAX -> id == Res.string.relax
                            HOME_PARAMS_SLEEP -> id == Res.string.sleep
                            HOME_PARAMS_ENERGIZE -> id == Res.string.energize
                            HOME_PARAMS_SAD -> id == Res.string.sad
                            HOME_PARAMS_ROMANCE -> id == Res.string.romance
                            HOME_PARAMS_FEEL_GOOD -> id == Res.string.feel_good
                            HOME_PARAMS_WORKOUT -> id == Res.string.workout
                            HOME_PARAMS_PARTY -> id == Res.string.party
                            HOME_PARAMS_COMMUTE -> id == Res.string.commute
                            HOME_PARAMS_FOCUS -> id == Res.string.focus
                            else -> id == Res.string.all
                        }
                        iOSGlassChip(
                            text = stringResource(id),
                            isSelected = isSelected,
                        ) {
                            when (id) {
                                Res.string.all -> viewModel.setParams(null)
                                Res.string.relax -> viewModel.setParams(HOME_PARAMS_RELAX)
                                Res.string.sleep -> viewModel.setParams(HOME_PARAMS_SLEEP)
                                Res.string.energize -> viewModel.setParams(HOME_PARAMS_ENERGIZE)
                                Res.string.sad -> viewModel.setParams(HOME_PARAMS_SAD)
                                Res.string.romance -> viewModel.setParams(HOME_PARAMS_ROMANCE)
                                Res.string.feel_good -> viewModel.setParams(HOME_PARAMS_FEEL_GOOD)
                                Res.string.workout -> viewModel.setParams(HOME_PARAMS_WORKOUT)
                                Res.string.party -> viewModel.setParams(HOME_PARAMS_PARTY)
                                Res.string.commute -> viewModel.setParams(HOME_PARAMS_COMMUTE)
                                Res.string.focus -> viewModel.setParams(HOME_PARAMS_FOCUS)
                            }
                        }
                    }
                }
            }
        }

        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = with(localDensity) { topAppBarHeightPx.toDp() + 18.dp })
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun HomeSectionCard(
    backdrop: PlatformBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .homeCleanLiquidGlass(
                backdrop = backdrop,
                shape = cardShape,
            )
            .padding(vertical = 12.dp),
        content = content,
    )
}

@Composable
fun iOSGlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val pillShape = RoundedCornerShape(50)

    val containerBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFE2E2E2),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f),
            ),
        )
    }

    val contentColor = if (isSelected) Color.Black else Color.White

    Box(
        modifier = Modifier
            .clip(pillShape)
            .background(brush = containerBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(Color.White, Color(0xFFD4D4D4))
                    } else {
                        listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.10f))
                    },
                ),
                shape = pillShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typo().labelSmall.copy(fontSize = 12.sp),
            color = contentColor,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController) {
    val hour = remember {
        val date = now().time
        date.hour
    }
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = typo().titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Text(
                    text = when (hour) {
                        in 6..12 -> stringResource(Res.string.good_morning)
                        in 13..17 -> stringResource(Res.string.good_afternoon)
                        in 18..23 -> stringResource(Res.string.good_evening)
                        else -> stringResource(Res.string.good_night)
                    },
                    style = typo().bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        },
        actions = {
            RippleIconButton(imageVector = SimpIcons.Notifications, tint = Color.White) {
                navController.navigate(NotificationDestination)
            }
            RippleIconButton(imageVector = SimpIcons.History, tint = Color.White) {
                navController.navigate(RecentlySongsDestination)
            }
            ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
            RippleIconButton(imageVector = SimpIcons.Settings, tint = Color.White) {
                navController.navigate(SettingsDestination)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
    )
}

@Composable
fun AccountLayout(
    accountName: String,
    url: String,
    backdrop: PlatformBackdrop,
) {
    val cardShape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .homeCleanLiquidGlass(
                backdrop = backdrop,
                shape = cardShape,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(Res.string.welcome_back),
            style = typo().bodyMedium,
            color = Color.White.copy(alpha = 0.70f),
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(url)
                    .crossfade(true)
                    .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
            )
            Text(
                text = accountName,
                style = typo().headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
    backdrop: PlatformBackdrop,
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    var widthDp by remember { mutableStateOf(0.dp) }
    var bottomSheetShow by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf<Track?>(null) }
    val localDensity = LocalDensity.current

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    val cardShape = RoundedCornerShape(22.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .homeCleanLiquidGlass(
                backdrop = backdrop,
                shape = cardShape,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .onGloballyPositioned { coordinates ->
                with(localDensity) {
                    widthDp = coordinates.size.width.toDp()
                }
            },
    ) {
        Text(
            text = stringResource(Res.string.let_s_start_with_a_radio),
            style = typo().bodySmall,
            color = Color.White.copy(alpha = 0.70f),
        )
        Text(
            text = stringResource(Res.string.quick_picks),
            style = typo().headlineMedium,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier.height(256.dp),
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
        ) {
            items(homeItem.contents, key = { it.hashCode() }) {
                if (it != null) {
                    QuickPicksItem(
                        onClick = {
                            val firstQueue: Track = it.toTrack()
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${it.videoId}",
                                    playlistName = "\"${it.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(firstQueue, type = Config.SONG_CLICK)
                        },
                        onLongClick = {
                            track = it.toTrack()
                            bottomSheetShow = true
                        },
                        data = it,
                        widthDp = widthDp,
                    )
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
        Text(
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
            color = Color.White.copy(alpha = 0.70f),
        )
        mood.sections.forEach { section ->
            val gridState = rememberLazyGridState()
            val flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = gridState))
            Text(
                text = section.title,
                style = typo().headlineMedium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
            )
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.height(210.dp),
                state = gridState,
                flingBehavior = flingBehavior,
            ) {
                items(section.items, key = { it.params }) { item ->
                    MoodMomentAndGenreHomeItem(
                        title = item.title,
                        stripeColor = item.stripeColor,
                    ) {
                        navController.navigate(MoodDestination(item.params))
                    }
                }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(Res.string.what_is_best_choice_today),
            style = typo().bodyMedium,
            color = Color.White.copy(alpha = 0.70f),
        )
        Text(
            text = stringResource(Res.string.chart),
            style = typo().headlineMedium,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )
    }
}

@Composable
fun ChartData(
    chart: Chart,
    navController: NavController,
) {
    var gridWidthDp by remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(localDensity) {
                gridWidthDp = coordinates.size.width.toDp()
            }
        },
    ) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(
                state = lazyListState,
                flingBehavior = snapperFlingBehavior,
            ) {
                items(item.playlists.size, key = { index ->
                    val data = item.playlists[index]
                    data.id + data.title + index
                }) {
                    HomeItemContentPlaylist(
                        onClick = {
                            navController.navigate(
                                PlaylistDestination(
                                    playlistId = item.playlists[it].id,
                                    isYourYouTubePlaylist = false,
                                ),
                            )
                        },
                        data = item.playlists[it],
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.top_artists),
            style = typo().headlineMedium,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId + index
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(
                    onClick = {
                        navController.navigate(ArtistDestination(channelId = data.browseId))
                    },
                    data = data,
                    widthDp = gridWidthDp,
                )
            }
        }
    }
}