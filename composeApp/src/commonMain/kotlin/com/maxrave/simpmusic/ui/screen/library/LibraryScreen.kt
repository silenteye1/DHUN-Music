package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.fetchDeviceLocalTracks
import com.maxrave.simpmusic.expect.rememberStoragePermissionLauncher
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibraryItem
import com.maxrave.simpmusic.ui.component.LibraryItemState
import com.maxrave.simpmusic.ui.component.LibraryItemType
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.screen.home.DynamicAmbientBackground
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.downloaded_playlists
import simpmusic.composeapp.generated.resources.favorite_playlists
import simpmusic.composeapp.generated.resources.favorite_podcasts
import simpmusic.composeapp.generated.resources.library
import simpmusic.composeapp.generated.resources.mix_for_you
import simpmusic.composeapp.generated.resources.no_YouTube_playlists
import simpmusic.composeapp.generated.resources.no_favorite_playlists
import simpmusic.composeapp.generated.resources.no_favorite_podcasts
import simpmusic.composeapp.generated.resources.no_playlists_added
import simpmusic.composeapp.generated.resources.no_playlists_downloaded
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import simpmusic.composeapp.generated.resources.wrapped
import simpmusic.composeapp.generated.resources.your_library
import simpmusic.composeapp.generated.resources.your_playlists
import simpmusic.composeapp.generated.resources.your_youtube_playlists

/**
 * Fast & Clean Liquid Glass Shader for LibraryScreen
 */
@Composable
fun Modifier.libraryCleanLiquidGlass(
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

@Composable
fun LibrarySectionCard(
    backdrop: PlatformBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .libraryCleanLiquidGlass(
                backdrop = backdrop,
                shape = cardShape,
            )
            .padding(vertical = 12.dp),
        content = content,
    )
}

@Composable
fun iOSGlassLibraryChip(
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
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    dataStoreManager: DataStoreManager = koinInject(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val customBackgroundUri by dataStoreManager.customBackgroundUri.collectAsStateWithLifecycle(initialValue = null)

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val localTrackingEnabled by viewModel.localTrackingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val monthlyRecaps by viewModel.monthlyRecaps.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val listCanvasSong by viewModel.listCanvasSong.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()

    var isShowingDeviceTracks by rememberSaveable { mutableStateOf(false) }
    var deviceTracks by remember { mutableStateOf<List<SongEntity>>(emptyList()) }
    var isScanningTracks by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val requestStoragePermission = rememberStoragePermissionLauncher { isGranted ->
        if (isGranted) {
            isShowingDeviceTracks = true
            if (deviceTracks.isEmpty()) {
                isScanningTracks = true
                coroutineScope.launch(Dispatchers.Default) {
                    deviceTracks = fetchDeviceLocalTracks()
                    isScanningTracks = false
                }
            }
        } else {
            viewModel.makeToast("Audio permission required to view local songs")
        }
    }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()

    val backdrop = rememberBackdrop(Color.Transparent)
    var topAppBarHeightPx by rememberSaveable { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(nowPlaying) {
        Logger.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    LaunchedEffect(isShowingDeviceTracks) {
        if (isShowingDeviceTracks && deviceTracks.isEmpty()) {
            isScanningTracks = true
            val tracks = withContext(Dispatchers.Default) {
                fetchDeviceLocalTracks()
            }
            deviceTracks = tracks
            isScanningTracks = false
        }
    }

    val chipRowState = rememberScrollState()
    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()

    LaunchedEffect(currentFilter) {
        when (currentFilter) {
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                if (youTubePlaylist.data.isNullOrEmpty()) {
                    viewModel.getYouTubePlaylist()
                }
            }
            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY)
            }
            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getCanvasSong()
                viewModel.getRecentlyAdded()
            }
            LibraryChipType.LOCAL_PLAYLIST -> {
                viewModel.getLocalPlaylist()
            }
            LibraryChipType.FAVORITE_PLAYLIST -> {
                viewModel.getPlaylistFavorite()
            }
            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                viewModel.getDownloadedPlaylist()
            }
            LibraryChipType.FAVORITE_PODCAST -> {
                viewModel.getFavoritePodcasts()
            }
            LibraryChipType.CHART -> {
                viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY)
            }
            LibraryChipType.WRAPPED -> {
                viewModel.getMonthlyRecaps()
            }
        }
    }

    val headerCapsuleShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508))
    ) {
        // ==================== 1. LIVE BACKDROP SOURCE CANVAS ====================
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
                                .memoryCachePolicy(CachePolicy.WRITE_ONLY)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    DynamicAmbientBackground(
                        modifier = Modifier.fillMaxSize(),
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        // ==================== 2. SCROLLABLE CONTENT ====================
        val calculatedHeaderSpacerHeight = if (topAppBarHeightPx > 0) {
            with(density) { topAppBarHeightPx.toDp() + 16.dp }
        } else {
            170.dp
        }

        if (isShowingDeviceTracks) {
            val deviceListState = rememberLazyListState()
            val isScrollingUp by deviceListState.isScrollingUp()
            LaunchedEffect(deviceListState) {
                snapshotFlow { deviceListState.firstVisibleItemIndex }
                    .collect {
                        if (it <= 1) {
                            onScrolling.invoke(true)
                        } else {
                            onScrolling.invoke(isScrollingUp)
                        }
                    }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 140.dp),
                state = deviceListState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Spacer(
                        modifier = Modifier
                            .statusBarsPadding()
                            .height(calculatedHeaderSpacerHeight)
                    )
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        LibrarySectionCard(backdrop = backdrop) {
                            LibraryItem(
                                state = LibraryItemState(
                                    type = LibraryItemType.RecentlyAdded(
                                        playingVideoId = nowPlaying,
                                    ),
                                    data = deviceTracks,
                                    isLoading = isScanningTracks,
                                ),
                                navController = navController,
                                selectionState = selectionState,
                            )
                        }
                    }
                }
                item {
                    EndOfPage()
                }
            }
        } else {
            Crossfade(
                targetState = currentFilter,
                modifier = Modifier.fillMaxSize(),
            ) { filter ->
                when (filter) {
                    LibraryChipType.YOUR_LIBRARY -> {
                        val state = rememberLazyListState()
                        val isScrollingUp by state.isScrollingUp()
                        LaunchedEffect(state) {
                            snapshotFlow { state.firstVisibleItemIndex }
                                .collect {
                                    if (it <= 1) {
                                        onScrolling.invoke(true)
                                    } else {
                                        onScrolling.invoke(isScrollingUp)
                                    }
                                }
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 140.dp),
                            state = state,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // Header Spacer: Stops capsule from overlapping the top cards
                            item {
                                Spacer(
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .height(calculatedHeaderSpacerHeight)
                                )
                            }

                            // Section 1: Tiling Box Glass Card
                            item {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    LibrarySectionCard(backdrop = backdrop) {
                                        LibraryTilingBox(navController)
                                    }
                                }
                            }

                            // Section 2: Canvas Song Glass Card
                            if (!listCanvasSong.data.isNullOrEmpty()) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        LibrarySectionCard(backdrop = backdrop) {
                                            LibraryItem(
                                                state = LibraryItemState(
                                                    type = LibraryItemType.CanvasSong,
                                                    data = listCanvasSong.data ?: emptyList(),
                                                    isLoading = listCanvasSong is LocalResource.Loading,
                                                ),
                                                navController = navController,
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 3: Recently Added Glass Card
                            item {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    LibrarySectionCard(backdrop = backdrop) {
                                        LibraryItem(
                                            state = LibraryItemState(
                                                type = LibraryItemType.RecentlyAdded(
                                                    playingVideoId = nowPlaying,
                                                ),
                                                data = recentlyAdded.data ?: emptyList(),
                                                isLoading = recentlyAdded is LocalResource.Loading,
                                            ),
                                            navController = navController,
                                            selectionState = selectionState,
                                        )
                                    }
                                }
                            }
                            item {
                                EndOfPage()
                            }
                        }
                    }

                    LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                        GridLibraryPlaylist(
                            navController = navController,
                            contentPadding = PaddingValues(top = calculatedHeaderSpacerHeight + 35.dp, bottom = 140.dp),
                            data = youTubePlaylist,
                            emptyText = Res.string.no_YouTube_playlists,
                            backdrop = backdrop,
                            onScrolling = onScrolling,
                            createNewPlaylist = {
                                showAddSheet = true
                            },
                        ) {
                            viewModel.getYouTubePlaylist()
                        }
                    }

                    LibraryChipType.YOUTUBE_MIX_FOR_YOU -> Unit

                    LibraryChipType.LOCAL_PLAYLIST -> {
                        GridLibraryPlaylist(
                            navController = navController,
                            contentPadding = PaddingValues(top = calculatedHeaderSpacerHeight + 35.dp, bottom = 140.dp),
                            data = yourLocalPlaylist,
                            emptyText = Res.string.no_playlists_added,
                            backdrop = backdrop,
                            onScrolling = onScrolling,
                            createNewPlaylist = {
                                showAddSheet = true
                            },
                        ) {
                            viewModel.getLocalPlaylist()
                        }
                    }

                    LibraryChipType.FAVORITE_PLAYLIST -> {
                        GridLibraryPlaylist(
                            navController = navController,
                            contentPadding = PaddingValues(top = calculatedHeaderSpacerHeight + 35.dp, bottom = 140.dp),
                            data = favoritePlaylist,
                            emptyText = Res.string.no_favorite_playlists,
                            backdrop = backdrop,
                            onScrolling = onScrolling,
                        ) {
                            viewModel.getPlaylistFavorite()
                        }
                    }

                    LibraryChipType.DOWNLOADED_PLAYLIST -> {
                        GridLibraryPlaylist(
                            navController = navController,
                            contentPadding = PaddingValues(top = calculatedHeaderSpacerHeight + 35.dp, bottom = 140.dp),
                            data = downloadedPlaylist,
                            emptyText = Res.string.no_playlists_downloaded,
                            backdrop = backdrop,
                            onScrolling = onScrolling,
                        ) {
                            viewModel.getDownloadedPlaylist()
                        }
                    }

                    LibraryChipType.FAVORITE_PODCAST -> {
                        GridLibraryPlaylist(
                            navController = navController,
                            contentPadding = PaddingValues(top = calculatedHeaderSpacerHeight + 35.dp, bottom = 140.dp),
                            data = favoritePodcasts,
                            emptyText = Res.string.no_favorite_podcasts,
                            backdrop = backdrop,
                            onScrolling = onScrolling,
                        ) {
                            viewModel.getFavoritePodcasts()
                        }
                    }

                    LibraryChipType.CHART -> Unit

                    LibraryChipType.WRAPPED -> {
                        LibraryWrappedTab(
                            navController = navController,
                            contentPadding = PaddingValues(top = calculatedHeaderSpacerHeight + 35.dp, bottom = 140.dp),
                            recaps = monthlyRecaps,
                            onScrolling = onScrolling,
                        ) {
                            viewModel.getMonthlyRecaps()
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
                    .libraryCleanLiquidGlass(
                        backdrop = backdrop,
                        shape = headerCapsuleShape,
                    ),
            ) {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Text(
                            text = stringResource(Res.string.library),
                            style = typo().titleMedium,
                            color = Color.White,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    navigationIcon = {
                        AnimatedVisibility(
                            !accountThumbnail.isNullOrEmpty(),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(accountThumbnail)
                                    .crossfade(550)
                                    .build(),
                                placeholder = rememberVectorPainter(SimpIcons.PeopleAlt),
                                error = rememberVectorPainter(SimpIcons.PeopleAlt),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                            )
                        }
                    },
                    actions = {
                        ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
                    },
                )

                AnimatedVisibility(visible = selectionState.isActive) {
                    SongSelectionTopAppBar(
                        state = selectionState,
                        windowInsets = WindowInsets(0),
                        onSelectAll = {
                            val activeList = if (isShowingDeviceTracks) {
                                deviceTracks.map { it.videoId }
                            } else {
                                (recentlyAdded.data ?: emptyList())
                                    .filterIsInstance<SongEntity>()
                                    .map { it.videoId }
                            }
                            selectionState.toggleSelectAll(activeList)
                        },
                        onOpenActions = { showSelectionSheet = true },
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    )
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(chipRowState)
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    iOSGlassLibraryChip(
                        text = "Device Songs",
                        isSelected = isShowingDeviceTracks,
                    ) {
                        if (!isShowingDeviceTracks) {
                            requestStoragePermission()
                        } else {
                            isShowingDeviceTracks = false
                        }
                    }

                    LibraryChipType.entries.forEach { type ->
                        if (type == LibraryChipType.YOUTUBE_MIX_FOR_YOU ||
                            type == LibraryChipType.CHART ||
                            (type == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST && !loggedIn) ||
                            (type == LibraryChipType.WRAPPED && !localTrackingEnabled)
                        ) {
                            return@forEach
                        }

                        val chipText = when (type) {
                            LibraryChipType.YOUR_LIBRARY -> stringResource(Res.string.your_library)
                            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> stringResource(Res.string.your_youtube_playlists)
                            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> stringResource(Res.string.mix_for_you)
                            LibraryChipType.LOCAL_PLAYLIST -> stringResource(Res.string.your_playlists)
                            LibraryChipType.FAVORITE_PLAYLIST -> stringResource(Res.string.favorite_playlists)
                            LibraryChipType.DOWNLOADED_PLAYLIST -> stringResource(Res.string.downloaded_playlists)
                            LibraryChipType.FAVORITE_PODCAST -> stringResource(Res.string.favorite_podcasts)
                            LibraryChipType.CHART -> ""
                            LibraryChipType.WRAPPED -> stringResource(Res.string.wrapped)
                        }

                        iOSGlassLibraryChip(
                            text = chipText,
                            isSelected = !isShowingDeviceTracks && type == currentFilter,
                        ) {
                            isShowingDeviceTracks = false
                            viewModel.setCurrentScreen(type)
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        var newTitle by remember { mutableStateOf("") }
        val showAddSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hideEditTitleBottomSheet: () -> Unit = {
            coroutineScope.launch {
                showAddSheetState.hide()
                showAddSheet = false
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = showAddSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .width(50.dp)
                            .height(4.dp),
                        colors = CardDefaults.cardColors().copy(
                            containerColor = MaterialTheme.colorScheme.outline,
                        ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.playlist_name))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                viewModel.makeToast(runBlocking { getString(Res.string.playlist_name_cannot_be_empty) })
                            } else {
                                if (currentFilter == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST) {
                                    viewModel.createYouTubePlaylist(newTitle) {
                                        viewModel.getYouTubePlaylist()
                                    }
                                } else {
                                    viewModel.createPlaylist(newTitle)
                                }
                                hideEditTitleBottomSheet()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.create))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (showSelectionSheet) {
        val selectedIds = selectionState.selected.toList()
        SelectedSongsBottomSheet(
            count = selectedIds.size,
            onDismiss = { showSelectionSheet = false },
            onPlayNext = {
                selectionViewModel.playNext(selectedIds)
                selectionState.exit()
            },
            onAddToQueue = {
                selectionViewModel.addToQueue(selectedIds)
                selectionState.exit()
            },
            onAddToPlaylist = { showSelectionAddToPlaylist = true },
            onDownload = {
                selectionViewModel.download(selectedIds)
                selectionState.exit()
            },
            onAddToFavorite = {
                selectionViewModel.addToFavorite(selectedIds)
                selectionState.exit()
            },
        )
    }

    if (showSelectionAddToPlaylist) {
        val selectedIds = selectionState.selected.toList()
        val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = localPlaylists,
            listYouTubePlaylist = emptyList(),
            onDismiss = { showSelectionAddToPlaylist = false },
            onClick = { playlist ->
                selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                selectionState.exit()
            },
            onYTPlaylistClick = {},
        )
    }
}