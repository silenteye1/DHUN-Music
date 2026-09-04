package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.LocalPlatformContext
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.common.CHART_SUPPORTED_COUNTRY
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DropdownButton
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
import com.maxrave.simpmusic.ui.component.ShareSavedLyricsDialog
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
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
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
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val regionChart by viewModel.regionCodeChart.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()

    val openAppTime by sharedViewModel.openAppTime.collectAsStateWithLifecycle()
    val shareLyricsPermissions by sharedViewModel.shareSavedLyrics.collectAsStateWithLifecycle()

    val isDark = LocalIsDarkTheme.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f

    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    var topHeaderColor by remember { mutableStateOf(backgroundColor) }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val mainHomeThumbnail by viewModel.mainHomeThumbnail.collectAsStateWithLifecycle()
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )

    LaunchedEffect(mainHomeThumbnail) {
        mainHomeThumbnail?.let {
            dominantColorState.updateFrom(Url(it))
        }
    }

    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }

    var showRequestShareLyricsPermissions by rememberSaveable { mutableStateOf(false) }
    var topAppBarHeightPx by rememberSaveable { mutableIntStateOf(0) }

    val hazeState = rememberHazeState(blurEnabled = true)

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
        Logger.w("HomeScreen", "onRefresh")
    }
    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
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
    LaunchedEffect(openAppTime, shareLyricsPermissions) {
        Logger.w("HomeScreen", "openAppTime: $openAppTime, shareLyricsPermissions: $shareLyricsPermissions")
        if ((openAppTime == 1 || openAppTime % 15 == 0) && openAppTime <= 60 && !shareLyricsPermissions) {
            showRequestShareLyricsPermissions = true
        } else {
            showRequestShareLyricsPermissions = false
        }
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
        Logger.d("HomeScreen", "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d("HomeScreen", "homeListState: $homeListState")
        Logger.d("HomeScreen", "Continuation: $continuation")
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {
            viewModel.getContinueHomeItem(
                continuation,
            )
        }
    }

    if (showRequestShareLyricsPermissions) {
        ShareSavedLyricsDialog(
            onDismissRequest = {
                showRequestShareLyricsPermissions = false
                sharedViewModel.onDoneReview(isDismissOnly = true)
            },
            onConfirm = { contributor ->
                sharedViewModel.onDoneRequestingShareLyrics(contributor)
            },
        )
    }

    if (shouldShowLogInAlert) {
        var doNotShowAgain by rememberSaveable { mutableStateOf(false) }
        AlertDialog(
            title = {
                Text(stringResource(Res.string.warning))
            },
            text = {
                Column {
                    Text(text = stringResource(Res.string.log_in_warning))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
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
            onDismissRequest = {
                viewModel.doneShowLogInAlert()
            },
        )
    }

    Box {
        PullToRefreshBox(
            modifier = Modifier.hazeSource(hazeState),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = with(LocalDensity.current) { topAppBarHeightPx.toDp() },
                            ),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    maxDistance = PullToRefreshDefaults.PositionalThreshold,
                )
            },
        ) {
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading) {
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
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        itemsIndexed(homeData, key = { _, item ->
                            item.hashCode().toString() + (mainHomeThumbnail ?: "nothumb")
                        }) { index, item ->
                            Box {
                                if (index == 0) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .matchParentSize()
                                                .angledGradientBackground(listOf(animatedColor, pageBackground), 25f),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(artworkScrimBrush(pageBackground)),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.padding(horizontal = 15.dp),
                                ) {
                                    if (index == 0) {
                                        Spacer(
                                            Modifier.height(
                                                with(LocalDensity.current) { topAppBarHeightPx.toDp() } + 48.dp,
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (item.title == stringResource(Res.string.quick_picks)) {
                                        AnimatedVisibility(
                                            visible =
                                                homeData.find {
                                                    it.title == stringResource(Res.string.quick_picks)
                                                } != null,
                                        ) {
                                            QuickPicks(
                                                homeItem =
                                                    (
                                                        homeData.find {
                                                            it.title == stringResource(Res.string.quick_picks)
                                                        } ?: return@AnimatedVisibility
                                                        ).let { content ->
                                                            content.copy(
                                                                contents =
                                                                    content.contents.mapNotNull { ct ->
                                                                        ct?.copy(
                                                                            artists =
                                                                                ct.artists?.let { art ->
                                                                                    if (art.size > 1) {
                                                                                        art.dropLast(1)
                                                                                    } else {
                                                                                        art
                                                                                    }
                                                                                },
                                                                        )
                                                                    },
                                                            )
                                                        },
                                                navController = navController,
                                                viewModel = viewModel,
                                            )
                                        }
                                    } else {
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
                                enter = expandVertically() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                CenterLoadingBox(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                )
                            }
                        }
                        if (homeListState == ListState.PAGINATION_EXHAUST) {
                            items(newRelease, key = { it.hashCode() }) {
                                AnimatedVisibility(
                                    visible = newRelease.isNotEmpty(),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 15.dp),
                                    ) {
                                        HomeItem(
                                            navController = navController,
                                            data = it,
                                        )
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = moodMomentAndGenre != null,
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 15.dp),
                                    ) {
                                        moodMomentAndGenre?.let {
                                            MoodMomentAndGenre(
                                                mood = it,
                                                navController = navController,
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Column(
                                    Modifier
                                        .padding(vertical = 10.dp)
                                        .padding(horizontal = 15.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    ChartTitle()
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(targetState = regionChart) {
                                        Logger.w("HomeScreen", "regionChart: $it")
                                        if (it != null) {
                                            DropdownButton(
                                                items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                                defaultSelected =
                                                    CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(
                                                        CHART_SUPPORTED_COUNTRY.items.indexOf(it),
                                                    )
                                                        ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                            ) {
                                                viewModel.exploreChart(
                                                    CHART_SUPPORTED_COUNTRY.items[
                                                        CHART_SUPPORTED_COUNTRY.itemsData.indexOf(
                                                            it,
                                                        ),
                                                    ],
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(
                                        targetState = chartLoading,
                                        label = "Chart",
                                    ) { loading ->
                                        if (!loading) {
                                            chart?.let {
                                                ChartData(
                                                    chart = it,
                                                    navController = navController,
                                                )
                                            }
                                        } else {
                                            CenterLoadingBox(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(400.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            EndOfPage()
                        }
                    }
                } else {
                    Column {
                        Spacer(
                            Modifier.height(
                                with(LocalDensity.current) { topAppBarHeightPx.toDp() },
                            ),
                        )
                        HomeShimmer()
                    }
                }
            }
        }

        // ================= BALANCED DYNAMIC LIQUID GLASS HEADER CAPSULE =================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .onGloballyPositioned { coordinates ->
                    topAppBarHeightPx = coordinates.size.height
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f))
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color.Black.copy(alpha = 0.55f),
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    }
                    .background(
                        if (isDark) Color(14, 14, 18).copy(alpha = 0.42f)
                        else Color.White.copy(alpha = 0.35f),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.25f),
                            ),
                        ),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(vertical = 8.dp),
            ) {
                // Top App Bar: Upar scroll karne par smoothly collapse hogi
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(200)),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(150)),
                ) {
                    Column {
                        HomeGlassHeaderBar(navController = navController)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Horizontal Filter Chips Row: Hamesha capsule ke andar visible rahegi
                Row(
                    modifier = Modifier
                        .horizontalScroll(chipRowState)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOfHomeChip.forEach { id ->
                        val isSelected =
                            when (params) {
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

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        if (isDark) Color.White.copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.65f)
                                    } else {
                                        Color.White.copy(alpha = 0.08f)
                                    },
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            if (isSelected) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.25f),
                                            if (isSelected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f),
                                        ),
                                    ),
                                    shape = CircleShape,
                                )
                                .clickable {
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
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(id),
                                style = typo().bodyMedium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeGlassHeaderBar(
    navController: NavController,
) {
    val hour =
        remember {
            val date = now().time
            date.hour
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(Res.string.app_name),
                style = typo().titleMedium,
                color = Color.White,
            )
            Text(
                text =
                    when (hour) {
                        in 6..12 -> stringResource(Res.string.good_morning)
                        in 13..17 -> stringResource(Res.string.good_afternoon)
                        in 18..23 -> stringResource(Res.string.good_evening)
                        else -> stringResource(Res.string.good_night)
                    },
                style = typo().bodySmall,
                color = Color.White.copy(alpha = 0.75f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleIconButton(
                onClick = { navController.navigate(NotificationDestination) },
            ) {
                Icon(
                    imageVector = SimpIcons.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
            GlassCircleIconButton(
                onClick = { navController.navigate(RecentlySongsDestination) },
            ) {
                Icon(
                    imageVector = SimpIcons.History,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
            GlassCircleIconButton(
                onClick = { navController.navigate(ListenTogetherDestination) },
            ) {
                ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
            }
            GlassCircleIconButton(
                onClick = { navController.navigate(SettingsDestination) },
            ) {
                Icon(
                    imageVector = SimpIcons.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
fun GlassCircleIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.08f),
                    ),
                ),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val density = LocalDensity.current
    var widthDp by remember { mutableStateOf(0.dp) }
    var bottomSheetShow by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf<Track?>(null) }

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    widthDp = (coordinates.size.width).toDp()
                }
            },
    ) {
        Text(
            text = stringResource(Res.string.let_s_start_with_a_radio),
            style = typo().bodySmall,
        )
        Text(
            text = stringResource(Res.string.quick_picks),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
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
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
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
    Column(
        Modifier.padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
        )
        mood.sections.forEach { section ->
            val gridState = rememberLazyGridState()
            val flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = gridState))
            Text(
                text = section.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
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
                        navController.navigate(
                            MoodDestination(item.params),
                        )
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
        )
        Text(
            text = stringResource(Res.string.chart),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
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
    val density = LocalDensity.current

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(density) {
                gridWidthDp = (coordinates.size.width).toDp()
            }
        },
    ) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(flingBehavior = snapperFlingBehavior) {
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
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
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
                        navController.navigate(
                            ArtistDestination(channelId = data.browseId),
                        )
                    },
                    data = data,
                    widthDp = gridWidthDp,
                )
            }
        }
    }
}