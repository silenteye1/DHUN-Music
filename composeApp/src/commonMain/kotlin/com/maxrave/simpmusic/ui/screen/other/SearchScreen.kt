package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.SearchResultType
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.toAppDeepLinkOrNull
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.MoodCategoryCard
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistFullWidthItems
import com.maxrave.simpmusic.ui.component.ShimmerSearchItem
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowOutward
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.SongRecognitionState
import com.maxrave.simpmusic.utils.SongRecognitionTrigger
import com.maxrave.simpmusic.utils.rememberSongRecognitionController
import com.maxrave.simpmusic.viewModel.SearchScreenUIState
import com.maxrave.simpmusic.viewModel.SearchType
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.toStringRes
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.clear_search_history
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.everything_you_need
import simpmusic.composeapp.generated.resources.in_search
import simpmusic.composeapp.generated.resources.no_results_found
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.search_for
import simpmusic.composeapp.generated.resources.search_for_songs_artists_albums_playlists_and_more
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.videos

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val focusManager = LocalFocusManager.current
    val searchScreenState by searchViewModel.searchScreenState.collectAsStateWithLifecycle()
    val uiState by searchViewModel.searchScreenUIState.collectAsStateWithLifecycle()
    val searchHistory by searchViewModel.searchHistory.collectAsStateWithLifecycle()
    val moodAndGenres by searchViewModel.moodAndGenres.collectAsStateWithLifecycle()
    val moodArtwork by searchViewModel.moodArtwork.collectAsStateWithLifecycle()

    var searchUIType by rememberSaveable { mutableStateOf(SearchUIType.EMPTY) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isSearchSubmitted by rememberSaveable { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val inputInteractionSource = remember { MutableInteractionSource() }
    val isInputFocused by inputInteractionSource.collectIsFocusedAsState()

    fun performSearch(query: String, type: SearchType) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return
        isSearchSubmitted = true
        isSearchActive = false
        focusManager.clearFocus()
        searchViewModel.insertSearchHistory(cleanQuery)
        searchViewModel.setSearchType(type)
        when (type) {
            SearchType.ALL -> searchViewModel.searchAll(cleanQuery)
            SearchType.SONGS -> searchViewModel.searchSongs(cleanQuery)
            SearchType.VIDEOS -> searchViewModel.searchVideos(cleanQuery)
            SearchType.ALBUMS -> searchViewModel.searchAlbums(cleanQuery)
            SearchType.ARTISTS -> searchViewModel.searchArtists(cleanQuery)
            SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(cleanQuery)
            SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(cleanQuery)
            SearchType.PODCASTS -> searchViewModel.searchPodcast(cleanQuery)
        }
    }

    val songRecognitionController = rememberSongRecognitionController { recognizedQuery ->
        searchText = recognizedQuery
        performSearch(recognizedQuery, SearchType.ALL)
    }

    val shouldListenFromTile by SongRecognitionTrigger.shouldListen.collectAsStateWithLifecycle()
    LaunchedEffect(shouldListenFromTile) {
        if (shouldListenFromTile) {
            SongRecognitionTrigger.consumeListenRequest()
            focusManager.clearFocus()
            isSearchActive = false
            songRecognitionController.startListening()
        }
    }

    LaunchedEffect(songRecognitionController.state) {
        if (songRecognitionController.state is SongRecognitionState.Success) {
            delay(850)
            songRecognitionController.reset()
        }
    }

    LaunchedEffect(isInputFocused) {
        if (isInputFocused) {
            isSearchActive = true
            isSearchSubmitted = false
        }
    }

    val screenInfo = getScreenSizeInfo()
    val isMobilePortrait = getPlatform() == Platform.Android && screenInfo.wDP < screenInfo.hDP
    val moodGridColumns = if (isMobilePortrait) 2 else 4

    val hazeState = rememberHazeState(blurEnabled = true)
    val suggestionsState = rememberLazyListState()
    val historyState = rememberLazyListState()
    val moodGridState = rememberLazyGridState()
    val resultsState = rememberLazyListState()
    var searchBarHeightPx by remember { mutableIntStateOf(0) }
    val searchBarHeight = with(LocalDensity.current) { searchBarHeightPx.toDp() }
    val isContentAtTop by remember {
        derivedStateOf {
            when (searchUIType) {
                SearchUIType.EMPTY ->
                    moodGridState.firstVisibleItemIndex == 0 && moodGridState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_HISTORY ->
                    historyState.firstVisibleItemIndex == 0 && historyState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_SUGGESTIONS ->
                    suggestionsState.firstVisibleItemIndex == 0 && suggestionsState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_RESULTS ->
                    resultsState.firstVisibleItemIndex == 0 && resultsState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    val searchForString = stringResource(Res.string.search_for)
    val songString = stringResource(Res.string.song).lowercase()
    val artistString = stringResource(Res.string.artists).lowercase()
    val albumString = stringResource(Res.string.albums).lowercase()
    val playlistString = stringResource(Res.string.playlists).lowercase()
    val videoString = stringResource(Res.string.videos).lowercase()
    val podcastString = stringResource(Res.string.podcasts).lowercase()

    val placeholderTexts =
        remember {
            listOf(
                "$searchForString $songString...",
                "$searchForString $artistString...",
                "$searchForString $albumString...",
                "$searchForString $playlistString...",
                "$searchForString $videoString...",
                "$searchForString $podcastString...",
            )
        }

    var currentPlaceholderIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isSearchActive) {
        while (!isSearchActive) {
            delay(3000)
            currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholderTexts.size
        }
    }

    var sheetSong by remember { mutableStateOf<SongEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    val currentVideoId by searchViewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val chipRowState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()

    val onMoreClick: (SongEntity) -> Unit = remember {
        { song ->
            sheetSong = song
            showBottomSheet = true
        }
    }

    val onSongItemClick: (Track, String) -> Unit = remember(searchText) {
        { track, clickType ->
            searchViewModel.setQueueData(
                QueueData.Data(
                    listTracks = arrayListOf(track),
                    firstPlayedTrack = track,
                    playlistId = "RDAMVM${track.videoId}",
                    playlistName = "\"${searchText}\" ${getStringBlocking(Res.string.in_search)}",
                    playlistType = PlaylistType.RADIO,
                    continuation = null,
                ),
            )
            searchViewModel.loadMediaItem(track, clickType)
        }
    }

    val onAddSongToQueue: (Track) -> Unit = remember {
        { track ->
            sharedViewModel.addListToQueue(arrayListOf(track))
        }
    }

    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && isSearchActive) {
            searchViewModel.suggestQuery(searchText)
        }
    }

    LaunchedEffect(isSearchActive, searchText, isSearchSubmitted) {
        searchUIType = when {
            isSearchSubmitted && searchText.isNotEmpty() -> SearchUIType.SEARCH_RESULTS
            isSearchActive && searchText.isNotEmpty() -> SearchUIType.SEARCH_SUGGESTIONS
            isSearchActive -> SearchUIType.SEARCH_HISTORY
            searchText.isEmpty() -> SearchUIType.EMPTY
            else -> SearchUIType.SEARCH_RESULTS
        }
    }

    if (songRecognitionController.state !is SongRecognitionState.Idle) {
        SongRecognitionLiquidModal(
            state = songRecognitionController.state,
            amplitude = songRecognitionController.amplitude,
            onRetry = { songRecognitionController.startListening() },
            onDismiss = { songRecognitionController.cancel() },
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
    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                sheetSong = null
            },
            navController = navController,
            song = sheetSong,
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Transparent),
    ) {
        Crossfade(
            targetState = searchUIType,
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
        ) {
            when (it) {
                SearchUIType.SEARCH_SUGGESTIONS -> {
                    LazyColumn(
                        Modifier.padding(horizontal = 16.dp),
                        state = suggestionsState,
                        contentPadding = PaddingValues(top = searchBarHeight, bottom = 10.dp),
                    ) {
                        itemsIndexed(
                            items = searchScreenState.suggestYTItems,
                            key = { index, item ->
                                val id = when (item) {
                                    is SongsResult -> item.videoId
                                    is VideosResult -> item.videoId
                                    is AlbumsResult -> item.browseId
                                    is ArtistsResult -> item.browseId
                                    is PlaylistsResult -> item.browseId
                                    else -> item.hashCode().toString()
                                }
                                "sugg_${index}_$id"
                            },
                        ) { _, item ->
                            SuggestItemRow(
                                searchResult = item,
                                onItemClick = { clickedItem ->
                                    when (clickedItem) {
                                        is SongsResult, is VideosResult -> {
                                            val firstTrack: Track = (clickedItem as? SongsResult)?.toTrack() ?: (clickedItem as VideosResult).toTrack()
                                            searchViewModel.setQueueData(
                                                QueueData.Data(
                                                    listTracks = arrayListOf(firstTrack),
                                                    firstPlayedTrack = firstTrack,
                                                    playlistId = "RDAMVM${firstTrack.videoId}",
                                                    playlistName = "\"${searchText}\" ${getStringBlocking(Res.string.in_search)}",
                                                    playlistType = PlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            searchViewModel.loadMediaItem(firstTrack, type = Config.SONG_CLICK)
                                        }

                                        is ArtistsResult -> {
                                            navController.navigate(
                                                ArtistDestination(clickedItem.browseId),
                                            )
                                        }

                                        is AlbumsResult -> {
                                            navController.navigate(
                                                AlbumDestination(clickedItem.browseId),
                                            )
                                        }

                                        is PlaylistsResult -> {
                                            navController.navigate(
                                                PlaylistDestination(
                                                    clickedItem.browseId,
                                                ),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        itemsIndexed(
                            items = searchScreenState.suggestQueries,
                            key = { index, q -> "q_${index}_$q" },
                        ) { _, suggestion ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(),
                                            onClick = {
                                                searchText = suggestion
                                                performSearch(suggestion, SearchType.ALL)
                                            },
                                        ).padding(horizontal = 12.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = suggestion,
                                    style = typo().bodyMedium,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        searchText = suggestion
                                        isSearchActive = true
                                        focusRequester.requestFocus()
                                    },
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.ArrowOutward,
                                        contentDescription = "Search suggestion",
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                        item {
                            EndOfPage(
                                withoutCredit = true,
                            )
                        }
                    }
                }

                SearchUIType.SEARCH_HISTORY -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                    ) {
                        LazyColumn(
                            state = historyState,
                            contentPadding = PaddingValues(top = searchBarHeight, bottom = 10.dp),
                        ) {
                            stickyHeader {
                                Crossfade(
                                    targetState = searchHistory.isNotEmpty(),
                                ) {
                                    if (it) {
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.background),
                                        ) {
                                            TextButton(
                                                onClick = { searchViewModel.deleteSearchHistory() },
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.clear_search_history),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            itemsIndexed(
                                items = searchHistory,
                                key = { index, hist -> "hist_${index}_$hist" },
                            ) { _, historyItem ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchText = historyItem
                                                performSearch(historyItem, SearchType.ALL)
                                            }.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.History,
                                        contentDescription = "Search history",
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                                    Text(
                                        text = historyItem,
                                        style = typo().bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            searchText = historyItem
                                            isSearchActive = true
                                            focusRequester.requestFocus()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.ArrowOutward,
                                            contentDescription = "Search suggestion",
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                            item {
                                EndOfPage(
                                    withoutCredit = true,
                                )
                            }
                        }
                    }
                }

                SearchUIType.EMPTY -> {
                    val mood = moodAndGenres
                    if (mood == null) {
                        CenterLoadingBox(Modifier.fillMaxSize())
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(moodGridColumns),
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .widthIn(max = 1100.dp)
                                        .padding(horizontal = 16.dp),
                                state = moodGridState,
                                contentPadding = PaddingValues(top = searchBarHeight),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 36.dp, bottom = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.everything_you_need),
                                            style = typo().titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = stringResource(Res.string.search_for_songs_artists_albums_playlists_and_more),
                                            style = typo().bodyMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                mood.sections.forEachIndexed { sIndex, section ->
                                    if (sIndex > 0) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Text(
                                                text = section.title,
                                                style = typo().titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.padding(top = 8.dp),
                                            )
                                        }
                                    }
                                    itemsIndexed(section.items, key = { itemIndex, it -> "mood_${sIndex}_${itemIndex}_${it.params}" }) { _, item ->
                                        LaunchedEffect(item.params) {
                                            searchViewModel.loadMoodArtwork(item.params)
                                        }
                                        MoodCategoryCard(
                                            title = item.title,
                                            artworkUrl = moodArtwork[item.params],
                                        ) {
                                            navController.navigate(MoodDestination(item.params))
                                        }
                                    }
                                }
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EndOfPage()
                                }
                            }
                        }
                    }
                }

                SearchUIType.SEARCH_RESULTS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            modifier = Modifier.fillMaxSize(),
                            state = pullToRefreshState,
                            onRefresh = {
                                performSearch(searchText, searchScreenState.searchType)
                            },
                            isRefreshing = uiState is SearchScreenUIState.Loading,
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullToRefreshState,
                                    isRefreshing = uiState is SearchScreenUIState.Loading,
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = searchBarHeight),
                                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                                    color = PullToRefreshDefaults.indicatorColor,
                                    maxDistance = PullToRefreshDefaults.PositionalThreshold,
                                )
                            },
                        ) {
                            Crossfade(targetState = uiState) { currentUiState ->
                                when (currentUiState) {
                                    is SearchScreenUIState.Loading -> {
                                        LazyColumn(
                                            contentPadding =
                                                PaddingValues(
                                                    top = searchBarHeight,
                                                    bottom = 10.dp,
                                                ),
                                        ) {
                                            items(10) {
                                                ShimmerSearchItem()
                                            }
                                        }
                                    }

                                    is SearchScreenUIState.Success -> {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            val currentResults =
                                                remember(searchScreenState) {
                                                    when (searchScreenState.searchType) {
                                                        SearchType.ALL -> {
                                                            val all = searchScreenState.searchAllResult
                                                            val tracks = all.filter { it is SongsResult || it is VideosResult }
                                                            val others = all.filter { it !is SongsResult && it !is VideosResult }
                                                            tracks + others
                                                        }
                                                        SearchType.SONGS -> searchScreenState.searchSongsResult
                                                        SearchType.VIDEOS -> searchScreenState.searchVideosResult
                                                        SearchType.ALBUMS -> searchScreenState.searchAlbumsResult
                                                        SearchType.ARTISTS -> searchScreenState.searchArtistsResult
                                                        SearchType.PLAYLISTS -> searchScreenState.searchPlaylistsResult
                                                        SearchType.FEATURED_PLAYLISTS -> searchScreenState.searchFeaturedPlaylistsResult
                                                        SearchType.PODCASTS -> searchScreenState.searchPodcastsResult
                                                    }
                                                }

                                            Crossfade(targetState = currentResults.isNotEmpty()) { hasResults ->
                                                if (hasResults) {
                                                    LazyColumn(
                                                        contentPadding =
                                                            PaddingValues(
                                                                start = 4.dp,
                                                                end = 4.dp,
                                                                top = searchBarHeight,
                                                                bottom = 10.dp,
                                                            ),
                                                        state = resultsState,
                                                    ) {
                                                        itemsIndexed(
                                                            items = currentResults,
                                                            key = { index, item ->
                                                                val id = when (item) {
                                                                    is SongsResult -> "song_${item.videoId}"
                                                                    is VideosResult -> "video_${item.videoId}"
                                                                    is AlbumsResult -> "album_${item.browseId}"
                                                                    is ArtistsResult -> "artist_${item.browseId}"
                                                                    is PlaylistsResult -> "playlist_${item.browseId}"
                                                                    else -> item.hashCode().toString()
                                                                }
                                                                "${index}_$id"
                                                            },
                                                        ) { _, result ->
                                                            when (result) {
                                                                is SongsResult -> {
                                                                    val track = remember(result) { result.toTrack() }
                                                                    val songEntity = remember(track) { track.toSongEntity() }
                                                                    SongFullWidthItems(
                                                                        track = track,
                                                                        isPlaying = result.videoId == currentVideoId,
                                                                        modifier = Modifier,
                                                                        onMoreClickListener = { onMoreClick(songEntity) },
                                                                        onClickListener = { onSongItemClick(track, Config.SONG_CLICK) },
                                                                        onAddToQueue = { onAddSongToQueue(track) },
                                                                        selectionMode = selectionState.isActive,
                                                                        isSelected = selectionState.isSelected(result.videoId),
                                                                        onLongClick = { selectionState.start(it) },
                                                                        onSelectToggle = { selectionState.toggle(it) },
                                                                    )
                                                                }

                                                                is VideosResult -> {
                                                                    val track = remember(result) { result.toTrack() }
                                                                    val songEntity = remember(track) { track.toSongEntity() }
                                                                    SongFullWidthItems(
                                                                        track = track,
                                                                        isPlaying = result.videoId == currentVideoId,
                                                                        modifier = Modifier,
                                                                        onMoreClickListener = { onMoreClick(songEntity) },
                                                                        onClickListener = { onSongItemClick(track, Config.VIDEO_CLICK) },
                                                                        onAddToQueue = { onAddSongToQueue(track) },
                                                                        selectionMode = selectionState.isActive,
                                                                        isSelected = selectionState.isSelected(result.videoId),
                                                                        onLongClick = { selectionState.start(it) },
                                                                        onSelectToggle = { selectionState.toggle(it) },
                                                                    )
                                                                }

                                                                is AlbumsResult -> {
                                                                    PlaylistFullWidthItems(
                                                                        data = result,
                                                                        onClickListener = {
                                                                            navController.navigate(
                                                                                AlbumDestination(
                                                                                    result.browseId,
                                                                                ),
                                                                            )
                                                                        },
                                                                    )
                                                                }

                                                                is ArtistsResult -> {
                                                                    ArtistFullWidthItems(
                                                                        data = result,
                                                                        onClickListener = {
                                                                            navController.navigate(
                                                                                ArtistDestination(
                                                                                    result.browseId,
                                                                                ),
                                                                            )
                                                                        },
                                                                    )
                                                                }

                                                                is PlaylistsResult -> {
                                                                    PlaylistFullWidthItems(
                                                                        data = result,
                                                                        onClickListener = {
                                                                            if (result.resultType == "Podcast") {
                                                                                navController.navigate(
                                                                                    PodcastDestination(
                                                                                        result.browseId,
                                                                                    ),
                                                                                )
                                                                            } else {
                                                                                navController.navigate(
                                                                                    PlaylistDestination(
                                                                                        result.browseId,
                                                                                    ),
                                                                                )
                                                                            }
                                                                        },
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        item { Spacer(modifier = Modifier.height(150.dp)) }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.no_results_found),
                                                            style = typo().titleMedium,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth(),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is SearchScreenUIState.Error -> {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Column(
                                                modifier = Modifier.align(Alignment.Center),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.error_occurred),
                                                    style = typo().titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(onClick = {
                                                    if (searchText.isNotEmpty()) {
                                                        performSearch(searchText, searchScreenState.searchType)
                                                    }
                                                }) {
                                                    Text(text = stringResource(Res.string.retry))
                                                }
                                            }
                                        }
                                    }

                                    SearchScreenUIState.Empty -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.no_results_found),
                                                style = typo().titleMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedContent(
            targetState = isContentAtTop,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { searchBarHeightPx = it.size.height },
            label = "search_bar_scrim",
        ) { atTop ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (atTop) {
                                Modifier.background(Color.Transparent)
                            } else {
                                Modifier.hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                                    blurEnabled = true
                                }
                            },
                        ).windowInsetsPadding(WindowInsets.statusBars)
                        .padding(vertical = 10.dp),
            ) {
                AnimatedVisibility(visible = selectionState.isActive) {
                    SongSelectionTopAppBar(
                        state = selectionState,
                        onSelectAll = {
                            val visible =
                                when (searchScreenState.searchType) {
                                    SearchType.SONGS -> searchScreenState.searchSongsResult.map { it.videoId }
                                    SearchType.VIDEOS -> searchScreenState.searchVideosResult.map { it.videoId }
                                    SearchType.ALL ->
                                        searchScreenState.searchAllResult.mapNotNull {
                                            (it as? SongsResult)?.videoId ?: (it as? VideosResult)?.videoId
                                        }
                                    else -> emptyList()
                                }
                            selectionState.toggleSelectAll(visible)
                        },
                        onOpenActions = { showSelectionSheet = true },
                        containerColor = Color.Transparent,
                        windowInsets = WindowInsets(0),
                    )
                }
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchText,
                            onQueryChange = { newText ->
                                searchText = newText
                                isSearchActive = true
                            },
                            onSearch = { query ->
                                val deepLink = query.toAppDeepLinkOrNull()
                                if (deepLink != null) {
                                    isSearchActive = false
                                    focusManager.clearFocus()
                                    sharedViewModel.setIntent(GenericIntent(data = deepLink))
                                } else {
                                    performSearch(query, SearchType.ALL)
                                }
                            },
                            expanded = false,
                            onExpandedChange = {},
                            enabled = true,
                            interactionSource = inputInteractionSource,
                            placeholder = {
                                AnimatedContent(
                                    targetState = currentPlaceholderIndex,
                                    transitionSpec = {
                                        (
                                            fadeIn(animationSpec = tween(500)) +
                                                slideInVertically { height -> height }
                                            ).togetherWith(
                                                fadeOut(animationSpec = tween(500)) +
                                                    slideOutVertically { height -> -height },
                                            )
                                    },
                                    label = "placeholder_animation",
                                ) { index ->
                                    Text(
                                        text = placeholderTexts[index],
                                        style = typo().labelMedium,
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = SimpIcons.Search,
                                    contentDescription = "Search",
                                )
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        modifier = Modifier.clip(CircleShape),
                                        onClick = {
                                            searchText = ""
                                            isSearchSubmitted = false
                                            isSearchActive = true
                                            focusRequester.requestFocus()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.Close,
                                            contentDescription = "Clear search",
                                        )
                                    }
                                } else {
                                    IconButton(
                                        modifier = Modifier.clip(CircleShape),
                                        onClick = {
                                            focusManager.clearFocus()
                                            isSearchActive = false
                                            songRecognitionController.startListening()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = rememberMicIcon(),
                                            contentDescription = "Identify Song",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    isSearchActive = true
                                }
                            }
                            .clickable {
                                isSearchActive = true
                                focusRequester.requestFocus()
                            }
                            .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    windowInsets = WindowInsets(0),
                    content = {},
                )
                AnimatedVisibility(visible = searchUIType == SearchUIType.SEARCH_RESULTS) {
                    Row(
                        modifier =
                            Modifier
                                .horizontalScroll(chipRowState)
                                .padding(top = 10.dp)
                                .padding(horizontal = 12.dp),
                    ) {
                        SearchType.entries.forEach { id ->
                            val isSelected = id == searchScreenState.searchType
                            Spacer(modifier = Modifier.width(4.dp))
                            Chip(
                                isAnimated = uiState is SearchScreenUIState.Loading,
                                isSelected = isSelected,
                                text = stringResource(id.toStringRes()),
                            ) {
                                if (searchText.isNotBlank()) {
                                    performSearch(searchText, id)
                                } else {
                                    searchViewModel.setSearchType(id)
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRecognitionLiquidModal(
    state: SongRecognitionState,
    amplitude: Float,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_pulse")
    val outerWave by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "outer_wave",
    )
    val innerWave by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "inner_wave",
    )
    val smoothAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(120),
        label = "smooth_amp",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
            border =
                BorderStroke(
                    width = 1.dp,
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.30f),
                                    Color.White.copy(alpha = 0.12f),
                                ),
                        ),
                ),
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        primaryColor.copy(alpha = 0.14f),
                                        tertiaryColor.copy(alpha = 0.06f),
                                        Color.Transparent,
                                    ),
                            ),
                        ).padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(136.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state is SongRecognitionState.Listening) {
                        Box(
                            modifier =
                                Modifier
                                    .size(126.dp)
                                    .scale(outerWave + smoothAmp * 0.25f)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.12f)),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .size(98.dp)
                                    .scale(innerWave + smoothAmp * 0.18f)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.22f)),
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(primaryColor, tertiaryColor),
                                    ),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state is SongRecognitionState.Processing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp,
                            )
                        } else {
                            AnimatedEqualizerBars(
                                color = MaterialTheme.colorScheme.onPrimary,
                                amplitude = smoothAmp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                when (state) {
                    is SongRecognitionState.Listening -> {
                        Text(
                            text = "Listening for music...",
                            style = typo().titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hold your device close to the speaker",
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is SongRecognitionState.Processing -> {
                        Text(
                            text = "Identifying song...",
                            style = typo().titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Matching audio fingerprint",
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is SongRecognitionState.Success -> {
                        Text(
                            text = state.title,
                            style = typo().titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.artist.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.artist,
                                style = typo().bodyMedium,
                                color = primaryColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    is SongRecognitionState.Error -> {
                        Text(
                            text = "Couldn't Identify",
                            style = typo().titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    SongRecognitionState.Idle -> Unit
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state is SongRecognitionState.Error) {
                        Button(
                            onClick = onRetry,
                            shape = CircleShape,
                        ) {
                            Text("Try Again")
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = if (state is SongRecognitionState.Success) "Done" else "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEqualizerBars(
    color: Color,
    amplitude: Float,
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "b1",
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "b2",
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "b3",
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse),
        label = "b4",
    )

    val boost = (1f + amplitude * 0.4f).coerceIn(1f, 1.35f)
    val heights = listOf(bar1, bar2, bar3, bar4)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(32.dp),
    ) {
        heights.forEach { h ->
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .height((28.dp * h * boost).coerceIn(8.dp, 32.dp))
                        .clip(CircleShape)
                        .background(color),
            )
        }
    }
}

@Composable
private fun rememberMicIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3z" +
                        "M17,11c0,2.76 -2.24,5 -5,5s-5,-2.24 -5,-5H5c0,3.53 2.61,6.43 6,6.92V21h2v-3.08c3.39,-0.49 6,-3.39 6,-6.92h-2z",
                ).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }
}

@Composable
fun SuggestItemRow(
    searchResult: SearchResultType,
    onItemClick: (SearchResultType) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onItemClick(searchResult) }
                .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val url =
            when (searchResult) {
                is SongsResult -> searchResult.thumbnails?.lastOrNull()?.url
                is AlbumsResult -> searchResult.thumbnails.lastOrNull()?.url
                is ArtistsResult -> searchResult.thumbnails.lastOrNull()?.url
                is PlaylistsResult -> searchResult.thumbnails.lastOrNull()?.url
                is VideosResult -> searchResult.thumbnails?.lastOrNull()?.url
                else -> null
            }

        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp)),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(url)
                        .size(100, 100)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(url)
                        .crossfade(true)
                        .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            if (searchResult is ArtistsResult) {
                                CircleShape
                            } else {
                                RoundedCornerShape(4.dp)
                            },
                        ),
            )
        }

        Spacer(modifier = Modifier.padding(horizontal = 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val title =
                when (searchResult) {
                    is SongsResult -> searchResult.title
                    is AlbumsResult -> searchResult.title
                    is ArtistsResult -> searchResult.artist
                    is PlaylistsResult -> searchResult.title
                    is VideosResult -> searchResult.title
                    else -> null
                } ?: "Unknown"

            Text(
                text = title,
                style = typo().labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))

            val subtitle =
                when (searchResult) {
                    is SongsResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    is AlbumsResult -> searchResult.artists.map { it.name }.connectArtists()
                    is PlaylistsResult -> searchResult.author.ifEmpty { "YouTube Music" }
                    is ArtistsResult -> stringResource(Res.string.artists)
                    is VideosResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    else -> null
                } ?: "Unknown"

            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

enum class SearchUIType {
    EMPTY,
    SEARCH_HISTORY,
    SEARCH_SUGGESTIONS,
    SEARCH_RESULTS,
}