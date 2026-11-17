package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.hexToColorOrNull
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.extension.toSquareThumbnailUrl
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItemArtist
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeItemVideo
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionState
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.PersonAdd
import com.maxrave.simpmusic.ui.icon.Sensors
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.MoreAlbumsDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ArtistScreenState
import com.maxrave.simpmusic.viewModel.ArtistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.featured_inArtist
import simpmusic.composeapp.generated.resources.more
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.popular
import simpmusic.composeapp.generated.resources.related_artists
import simpmusic.composeapp.generated.resources.singles
import simpmusic.composeapp.generated.resources.unknown
import simpmusic.composeapp.generated.resources.videos

@Composable
@ExperimentalMaterial3Api
fun ArtistScreen(
    channelId: String,
    viewModel: ArtistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val artistScreenState by viewModel.artistScreenState.collectAsStateWithLifecycle()
    val isFollowed by viewModel.followed.collectAsStateWithLifecycle()
    val canvasUrl by viewModel.canvasUrl.collectAsStateWithLifecycle()
    val artistLogo by viewModel.artistLogo.collectAsStateWithLifecycle()

    val playingTrack by remember {
        sharedViewModel.nowPlayingState.map { it?.track?.videoId }
    }.collectAsState(null)

    var choosingTrack by remember {
        mutableStateOf<Track?>(null)
    }
    var showBottomSheet by remember {
        mutableStateOf(false)
    }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(channelId) {
        if (channelId != artistScreenState.data.channelId) {
            viewModel.browseArtist(channelId)
        }
    }

    val screenInfo = getScreenSizeInfo()
    val isPortrait = screenInfo.wDP < screenInfo.hDP

    val paletteState = com.kmpalette.rememberPaletteState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }
    val currentImageUrl = (artistScreenState as? ArtistScreenState.Success)?.data?.imageUrl

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && currentImageUrl != null && paletteGeneratedFor != currentImageUrl) {
            paletteState.generate(bm)
            paletteGeneratedFor = currentImageUrl
        }
    }

    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()
    val sectionTint = paletteState.palette.getColorFromPalette()
    val artistAccent = artistLogo?.bgColorHex?.hexToColorOrNull() ?: Color.White

    val hazeState = rememberHazeState(blurEnabled = true)
    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf { lazyState.firstVisibleItemIndex == 0 }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }

    Crossfade(artistScreenState) { state ->
        when (state) {
            is ArtistScreenState.Loading -> {
                Box(Modifier.fillMaxSize()) {
                    CenterLoadingBox(
                        Modifier
                            .align(Alignment.Center),
                    )
                }
            }

            is ArtistScreenState.Success -> {
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(mutedPaletteBg)
                                .hazeSource(hazeState),
                        state = lazyState,
                    ) {
                        item(contentType = "header") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy((-36).dp),
                            ) {
                                val artworkBackdrop = rememberBackdrop(Color.Black)
                                val headerHaze = rememberHazeState(blurEnabled = true)
                                val headerImageUrl =
                                    state.data.imageUrl?.let {
                                        if (isPortrait) it.toSquareThumbnailUrl() else it
                                    }
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isPortrait) {
                                                    Modifier.aspectRatio(1f)
                                                } else {
                                                    Modifier.height((screenInfo.hDP / 2).dp)
                                                },
                                            ),
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().clipToBounds().layerBackdrop(artworkBackdrop)) {
                                        Box(modifier = Modifier.fillMaxSize().hazeSource(headerHaze)) {
                                            AsyncImage(
                                                model =
                                                    ImageRequest
                                                        .Builder(LocalPlatformContext.current)
                                                        .data(headerImageUrl)
                                                        .diskCachePolicy(CachePolicy.ENABLED)
                                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                                        .diskCacheKey(headerImageUrl)
                                                        .memoryCacheKey(headerImageUrl)
                                                        .crossfade(false)
                                                        .build(),
                                                placeholder = rememberHolderPainter(),
                                                error = rememberHolderPainter(),
                                                contentDescription = null,
                                                contentScale =
                                                    if (isPortrait) ContentScale.FillWidth else ContentScale.Crop,
                                                onSuccess = {
                                                    bitmap = it.result.image.toImageBitmap()
                                                },
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .alpha(if (canvasUrl != null) 0f else 1f),
                                            )
                                            canvasUrl?.let { canvas ->
                                                MediaPlayerView(
                                                    url = canvas.first,
                                                    modifier = Modifier.fillMaxSize(),
                                                    cropToBounds = true,
                                                )
                                            }
                                        }
                                        if (getPlatform() == Platform.Android) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .hazeEffect(headerHaze) {
                                                            blurRadius = 32.dp
                                                            progressive =
                                                                HazeProgressive.verticalGradient(
                                                                    startIntensity = 0f,
                                                                    endIntensity = 1f,
                                                                )
                                                        },
                                            )
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(
                                                        if (isPortrait) {
                                                            (screenInfo.wDP * 0.7f).dp
                                                        } else {
                                                            (screenInfo.hDP * 0.35f).dp
                                                        },
                                                    )
                                                    .align(Alignment.BottomCenter)
                                                    .background(artworkScrimBrush(mutedPaletteBg)),
                                        )
                                        Column(
                                            modifier =
                                                Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .offset(y = (-36).dp)
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp)
                                                    .padding(bottom = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            val logo = artistLogo
                                            if (logo != null) {
                                                AsyncImage(
                                                    model = logo.logoUrl,
                                                    contentDescription = state.data.title,
                                                    contentScale = ContentScale.Fit,
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth(0.7f)
                                                            .heightIn(max = 84.dp),
                                                )
                                            } else {
                                                Text(
                                                    text = state.data.title ?: stringResource(Res.string.unknown),
                                                    style = typo().titleLarge,
                                                    color = Color.White,
                                                    maxLines = 2,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                            val meta =
                                                listOfNotNull(
                                                    state.data.subscribers?.takeIf { it.isNotBlank() },
                                                    state.data.playCount?.takeIf { it.isNotBlank() },
                                                ).joinToString(" · ")
                                            if (meta.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = meta,
                                                    style = typo().bodyMedium,
                                                    color = Color(0xC4FFFFFF),
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                    LiquidGlassIconButton(
                                        backdrop = artworkBackdrop,
                                        imageVector = SimpIcons.ArrowBackIosNew,
                                        shape = RoundedCornerShape(24.dp),
                                        highlight = Highlight(width = 1.dp),
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopStart)
                                                .padding(12.dp)
                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                .size(48.dp),
                                    ) {
                                        navController.navigateUp()
                                    }
                                }

                                // Action Row: [Radio (if available)] [Shuffle (if available)] [Subscribe pill]
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 32.dp)
                                            .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val radioParam = state.data.radioParam
                                    if (radioParam != null) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, artistAccent, CircleShape)
                                                    .clickable {
                                                        viewModel.onRadioClick(radioParam)
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Sensors,
                                                contentDescription = "Radio",
                                                tint = artistAccent,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                    val shuffleParam = state.data.shuffleParam
                                    if (shuffleParam != null) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(artistAccent)
                                                    .clickable {
                                                        viewModel.onShuffleClick(shuffleParam)
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Shuffle,
                                                contentDescription = "Shuffle",
                                                tint = mutedPaletteBg,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    }
                                    // YouTube-style Subscribe / Subscribed Pill Button
                                    Row(
                                        modifier =
                                            Modifier
                                                .height(48.dp)
                                                .clip(CircleShape)
                                                .background(if (isFollowed) Color.White.copy(alpha = 0.16f) else artistAccent)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (isFollowed) Color.White.copy(alpha = 0.35f) else artistAccent,
                                                    shape = CircleShape,
                                                )
                                                .clickable {
                                                    viewModel.updateFollowed(
                                                        if (isFollowed) 0 else 1,
                                                        state.data.channelId ?: return@clickable,
                                                    )
                                                }
                                                .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = if (isFollowed) SimpIcons.Check else SimpIcons.PersonAdd,
                                            contentDescription = if (isFollowed) "Subscribed" else "Subscribe",
                                            tint = if (isFollowed) Color.White else mutedPaletteBg,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = if (isFollowed) "Subscribed" else "Subscribe",
                                            style = typo().labelMedium,
                                            color = if (isFollowed) Color.White else mutedPaletteBg,
                                        )
                                    }
                                }
                            }
                        }
                        item(contentType = "sections") {
                            ArtistSections(
                                state = state,
                                selectionState = selectionState,
                                playingTrack = playingTrack,
                                descriptionTint = sectionTint,
                                navController = navController,
                                viewModel = viewModel,
                                sharedViewModel = sharedViewModel,
                                onTrackMore = { track ->
                                    choosingTrack = track
                                    showBottomSheet = true
                                },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = shouldHideTopBar && !selectionState.isActive,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                    ) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = state.data.title ?: "",
                                    style = typo().titleMedium,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
                            },
                            navigationIcon = {
                                Box(Modifier.padding(horizontal = 5.dp)) {
                                    IconButton(onClick = { navController.navigateUp() }) {
                                        Icon(
                                            imageVector = SimpIcons.ArrowBackIosNew,
                                            contentDescription = "Back",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                ),
                            modifier =
                                Modifier.hazeEffect(hazeState) {
                                    blurEnabled = true
                                    blurRadius = 24.dp
                                    backgroundColor = mutedPaletteBg
                                    tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                                },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selectionState.isActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    SongSelectionTopAppBar(
                        state = selectionState,
                        onSelectAll = {
                            selectionState.toggleSelectAll(
                                (artistScreenState as? ArtistScreenState.Success)
                                    ?.data
                                    ?.popularSongs
                                    ?.map { it.videoId }
                                    ?: emptyList(),
                            )
                        },
                        onOpenActions = { showSelectionSheet = true },
                        containerColor = Color.Black,
                    )
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
                if (showBottomSheet && choosingTrack != null) {
                    NowPlayingBottomSheet(
                        onDismiss = {
                            showBottomSheet = false
                            choosingTrack = null
                        },
                        navController = navController,
                        song = choosingTrack?.toSongEntity(),
                    )
                }
            }

            is ArtistScreenState.Error -> {
                viewModel.makeToast(state.message ?: stringResource(Res.string.error))
                navController.navigateUp()
            }
        }
    }
}

@Composable
private fun ArtistSections(
    state: ArtistScreenState.Success,
    selectionState: SongSelectionState,
    playingTrack: String?,
    descriptionTint: Color,
    navController: NavController,
    viewModel: ArtistViewModel,
    sharedViewModel: SharedViewModel,
    onTrackMore: (Track) -> Unit,
) {
    Column {
        // Podcasts (for Podcast Channels)
        AnimatedVisibility(state.data.podcasts.isNotEmpty()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.podcasts),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                    )
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.podcasts) { podcast ->
                        HomeItemContentPlaylist(
                            forceDark = true,
                            onClick = {
                                if (podcast.id.startsWith("MPSP")) {
                                    navController.navigate(
                                        PodcastDestination(
                                            podcastId = podcast.id,
                                        ),
                                    )
                                } else {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = podcast.id,
                                        ),
                                    )
                                }
                            },
                            data = podcast,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Popular Songs
        AnimatedVisibility(state.data.popularSongs.isNotEmpty()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.popular),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val id = state.data.listSongParam
                            if (id != null) {
                                navController.navigate(PlaylistDestination(id))
                            } else {
                                viewModel.makeToast(runBlocking { getString(Res.string.error) })
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                state.data.popularSongs.forEach { song ->
                    SongFullWidthItems(
                        forceDark = true,
                        track = song,
                        isPlaying = song.videoId == playingTrack,
                        modifier = Modifier.fillMaxWidth(),
                        onMoreClickListener = {
                            onTrackMore(song)
                        },
                        onClickListener = {
                            val firstQueue: Track = song
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${song.videoId}",
                                    playlistName = "\"${state.data.title ?: ""}\" ${getStringBlocking(Res.string.popular)}",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        onAddToQueue = {
                            sharedViewModel.addListToQueue(
                                arrayListOf(song),
                            )
                        },
                        selectionMode = selectionState.isActive,
                        isSelected = selectionState.isSelected(song.videoId),
                        onLongClick = { selectionState.start(it) },
                        onSelectToggle = { selectionState.toggle(it) },
                    )
                }
            }
        }

        // Singles
        AnimatedVisibility(
            state.data.singles != null &&
                state.data.singles!!
                    .results
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.singles),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            if (state.data.channelId != null) {
                                val id = "MPAD${state.data.channelId}"
                                navController.navigate(
                                    MoreAlbumsDestination(
                                        id = id,
                                        type = MoreAlbumsDestination.SINGLE_TYPE,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(getStringBlocking(Res.string.error))
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.singles?.results ?: emptyList()) { single ->
                        HomeItemContentPlaylist(
                            forceDark = true,
                            onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        single.browseId,
                                    ),
                                )
                            },
                            data = single,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Albums
        AnimatedVisibility(
            state.data.albums != null &&
                state.data.albums!!
                    .results
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.albums),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            if (state.data.channelId != null) {
                                val id = "MPAD${state.data.channelId}"
                                navController.navigate(
                                    MoreAlbumsDestination(
                                        id = id,
                                        type = MoreAlbumsDestination.ALBUM_TYPE,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(getStringBlocking(Res.string.error))
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.albums?.results ?: emptyList()) { album ->
                        HomeItemContentPlaylist(
                            forceDark = true,
                            onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        browseId = album.browseId,
                                    ),
                                )
                            },
                            data = album,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Videos
        AnimatedVisibility(
            state.data.video != null &&
                state.data.video!!
                    .video
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.videos),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val videoListParam = state.data.video?.videoListParam
                            if (videoListParam != null) {
                                navController.navigate(
                                    PlaylistDestination(
                                        videoListParam,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(getStringBlocking(Res.string.error))
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.video?.video ?: emptyList()) { video ->
                        HomeItemVideo(
                            forceDark = true,
                            onClick = {
                                val firstQueue: Track = video
                                viewModel.setQueueData(
                                    QueueData.Data(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM${video.videoId}",
                                        playlistName = (state.data.title ?: "") + getStringBlocking(Res.string.videos),
                                        playlistType = PlaylistType.RADIO,
                                        continuation = null,
                                    ),
                                )
                                viewModel.loadMediaItem(
                                    firstQueue,
                                    type = Config.VIDEO_CLICK,
                                )
                            },
                            onLongClick = {
                                onTrackMore(video)
                            },
                            data =
                                Content(
                                    album = null,
                                    artists = video.artists,
                                    description = null,
                                    isExplicit = video.isExplicit,
                                    playlistId = null,
                                    browseId = null,
                                    thumbnails = video.thumbnails ?: emptyList(),
                                    title = video.title,
                                    videoId = video.videoId,
                                    views = video.views,
                                ),
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Feature on
        AnimatedVisibility(state.data.featuredOn.isNotEmpty()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.featured_inArtist),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                    )
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.featuredOn) { feature ->
                        HomeItemContentPlaylist(
                            forceDark = true,
                            onClick = {
                                if (feature.id.startsWith("MPSP")) {
                                    navController.navigate(
                                        PodcastDestination(
                                            podcastId = feature.id,
                                        ),
                                    )
                                } else {
                                    navController.navigate(
                                        PlaylistDestination(
                                            feature.id,
                                        ),
                                    )
                                }
                            },
                            data = feature,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Related
        AnimatedVisibility(
            state.data.related != null &&
                state.data.related!!
                    .results
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.related_artists),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                    )
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.related?.results ?: emptyList()) { related ->
                        HomeItemArtist(
                            forceDark = true,
                            onClick = {
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = related.browseId,
                                    ),
                                )
                            },
                            data =
                                Content(
                                    album = null,
                                    artists =
                                        listOf(
                                            Artist(
                                                id = related.browseId,
                                                name = related.title,
                                            ),
                                        ),
                                    description = related.subscribers,
                                    isExplicit = null,
                                    playlistId = null,
                                    browseId = related.browseId,
                                    thumbnails = related.thumbnails,
                                    title = related.title,
                                    videoId = null,
                                    views = null,
                                    durationSeconds = null,
                                    radio = null,
                                ),
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(Res.string.description),
                style = typo().labelMedium,
                color = Color.White,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
            )
        }
        val urlHandler = LocalUriHandler.current
        ElevatedCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.elevatedCardColors().copy(
                    containerColor = descriptionTint.rgbFactor(0.5f),
                ),
        ) {
            DescriptionView(
                modifier = Modifier.padding(16.dp),
                text = state.data.description ?: stringResource(Res.string.no_description),
                limitLine = 5,
                onTimeClicked = {},
                onURLClicked = { url ->
                    urlHandler.openUri(url)
                },
            )
        }
        EndOfPage()
    }
}