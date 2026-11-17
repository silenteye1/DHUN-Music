package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeDataCombine
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.music_video
import simpmusic.composeapp.generated.resources.new_release
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.view_count

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
    private val searchRepository: SearchRepository,
) : BaseViewModel() {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private var _homeListState = MutableStateFlow<ListState>(ListState.IDLE)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null
    private var podcastChannelJob: Job? = null
    private var allPodcastChannelsJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(null)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<List<HomeItem>> = MutableStateFlow(arrayListOf())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val loading = MutableStateFlow<Boolean>(true)
    val loadingChart = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // Podcast Channels state (for Podcasts tab)
    private val _podcastChannels: MutableStateFlow<List<Content>> = MutableStateFlow(emptyList())
    val podcastChannels: StateFlow<List<Content>> = _podcastChannels

    private val _podcastChannelsLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val podcastChannelsLoading: StateFlow<Boolean> = _podcastChannelsLoading

    private val _podcastChannelQuery: MutableStateFlow<String> = MutableStateFlow("")
    val podcastChannelQuery: StateFlow<String> = _podcastChannelQuery

    // All Podcast Channels state (for "All" A-Z Directory)
    private val _allPodcastChannels: MutableStateFlow<List<Content>> = MutableStateFlow(emptyList())
    val allPodcastChannels: StateFlow<List<Content>> = _allPodcastChannels

    private val _allPodcastChannelsLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val allPodcastChannelsLoading: StateFlow<Boolean> = _allPodcastChannelsLoading

    // For showing alert that should log in to YouTube
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _mainHomeThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainHomeThumbnail: StateFlow<String?> = _mainHomeThumbnail

    private val musicChannelBlacklist = listOf(
        "music", "records", "vevo", "topic", "t-series", "tseries",
        "sony", "zee", "yrf", "saregama", "tips", "eros", "shemaroo",
        "speed records", "white hill", "desi melodi", "universal", "warner",
        "atlantic", "columbia", "republic", "interscope", "def jam", "coke studio",
    )

    private val musicVideoBlacklist = listOf(
        "official video", "official music video", "full video song", "lyrical",
        "full song", "audio song", "jukebox", "remix", "lofi", "slowed",
        "ringtone", "whatsapp status", "mashup", "cover song", "ft.", "feat.",
    )

    private fun isMusicChannelOrTitle(name: String, contextTitle: String = ""): Boolean {
        val nameLower = name.lowercase()
        val titleLower = contextTitle.lowercase()
        if (musicChannelBlacklist.any { nameLower.contains(it) }) return true
        if (contextTitle.isNotEmpty() && musicVideoBlacklist.any { titleLower.contains(it) }) return true
        return false
    }

    init {
        if (runBlocking { dataStoreManager.cookie.first() }.isEmpty() &&
            runBlocking {
                dataStoreManager.shouldShowLogInRequiredAlert.first() == TRUE
            }
        ) {
            _showLogInAlert.update { true }
        }
        homeJob = Job()
        viewModelScope.launch {
            regionCodeChart.value = dataStoreManager.chartKey.first()
            exploreChart(regionCodeChart.value ?: "ZZ")
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            val job1 =
                launch {
                    dataStoreManager.location.distinctUntilChanged().collect {
                        regionCode = it
                        getHomeItemList(params.value)
                    }
                }
            val job2 =
                launch {
                    dataStoreManager.language.distinctUntilChanged().collect {
                        language = it
                        getHomeItemList(params.value)
                    }
                }
            val job3 =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collect {
                        getHomeItemList(params.value)
                        _accountInfo.emit(
                            Pair(
                                dataStoreManager.getString("AccountName").first(),
                                dataStoreManager.getString("AccountThumbUrl").first(),
                            ),
                        )
                    }
                }
            val job4 =
                launch {
                    params.collectLatest {
                        getHomeItemList(it)
                        if (it == HOME_PARAMS_PODCASTS) {
                            searchPodcastChannels(_podcastChannelQuery.value)
                        }
                    }
                }
            val job5 =
                launch {
                    dataStoreManager
                        .cookie
                        .distinctUntilChanged()
                        .collectLatest {
                            if (it.isNotEmpty()) {
                                Logger.w(tag, "Cookie changed, refreshing home")
                                loading.value = true
                                delay(1000)
                                getHomeItemList(params.value)
                            }
                        }
                }
            val job6 =
                launch {
                    homeItemList.collectLatest { list ->
                        _mainHomeThumbnail.value =
                            list
                                .firstOrNull()
                                ?.contents
                                ?.firstOrNull()
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url
                        if (params.value == HOME_PARAMS_PODCASTS && _podcastChannels.value.isEmpty() && list.isNotEmpty()) {
                            searchPodcastChannels(_podcastChannelQuery.value)
                        }
                    }
                }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
        }
    }

    private suspend fun fetchChannelsForQuery(
        rawQuery: String,
        targetMap: MutableMap<String, Content>,
        strictMatch: Boolean,
    ) {
        val cleanQuery = rawQuery.trim()
        val queryTokens =
            if (strictMatch && cleanQuery.isNotEmpty()) {
                cleanQuery.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

        val podcastSearchQuery =
            if (cleanQuery.isEmpty()) {
                "podcast"
            } else if (cleanQuery.contains("podcast", ignoreCase = true)) {
                cleanQuery
            } else {
                "$cleanQuery podcast"
            }

        val podcastRes = searchRepository.getSearchDataPodcast(podcastSearchQuery).firstOrNull()
        val videoRes = searchRepository.getSearchDataVideo(podcastSearchQuery).firstOrNull()

        if (podcastRes is Resource.Success) {
            podcastRes.data?.forEach { item ->
                val rawAuthor = item.author.substringAfterLast("•").trim()
                val showTitle = item.title.trim()
                val targetId = item.browseId

                val candidateName =
                    when {
                        rawAuthor.isNotEmpty() &&
                            !rawAuthor.equals("Podcast", ignoreCase = true) &&
                            !rawAuthor.equals("YouTube Music", ignoreCase = true) -> rawAuthor
                        else -> showTitle
                    }

                if (targetId.isNotBlank() &&
                    candidateName.isNotEmpty() &&
                    !isMusicChannelOrTitle(candidateName, showTitle)
                ) {
                    val matchesQuery =
                        queryTokens.isEmpty() ||
                            queryTokens.any { token ->
                                candidateName.contains(token, ignoreCase = true) ||
                                    showTitle.contains(token, ignoreCase = true) ||
                                    rawAuthor.contains(token, ignoreCase = true)
                            }

                    if (matchesQuery && !targetMap.containsKey(candidateName.lowercase())) {
                        targetMap[candidateName.lowercase()] =
                            Content(
                                album = null,
                                artists = listOf(Artist(id = targetId, name = candidateName)),
                                description = null,
                                isExplicit = null,
                                playlistId = null,
                                browseId = targetId,
                                thumbnails = item.thumbnails,
                                title = candidateName,
                                videoId = null,
                                views = null,
                                radio = null,
                            )
                    }
                }
            }
        }

        if (videoRes is Resource.Success) {
            videoRes.data?.forEach { video ->
                val videoTitle = video.title.orEmpty()
                val isPodcastVideo =
                    videoTitle.contains("podcast", ignoreCase = true) ||
                        videoTitle.contains("episode", ignoreCase = true) ||
                        videoTitle.contains("ep.", ignoreCase = true) ||
                        videoTitle.contains("ep ", ignoreCase = true) ||
                        videoTitle.contains("interview", ignoreCase = true) ||
                        videoTitle.contains("conversation", ignoreCase = true) ||
                        videoTitle.contains("show", ignoreCase = true) ||
                        !videoTitle.contains("ft.", ignoreCase = true)

                if (isPodcastVideo) {
                    video.artists?.forEach { artist ->
                        val cId = artist.id
                        val cName = artist.name.trim()
                        if (!cId.isNullOrBlank() &&
                            cId.startsWith("UC") &&
                            cName.isNotEmpty() &&
                            !isMusicChannelOrTitle(cName, videoTitle)
                        ) {
                            val matchesQuery =
                                queryTokens.isEmpty() ||
                                    queryTokens.any { token ->
                                        cName.contains(token, ignoreCase = true) ||
                                            videoTitle.contains(token, ignoreCase = true)
                                    }

                            if (matchesQuery) {
                                targetMap[cName.lowercase()] =
                                    Content(
                                        album = null,
                                        artists = listOf(Artist(id = cId, name = cName)),
                                        description = null,
                                        isExplicit = null,
                                        playlistId = null,
                                        browseId = cId,
                                        thumbnails = video.thumbnails ?: emptyList(),
                                        title = cName,
                                        videoId = null,
                                        views = null,
                                        radio = null,
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    fun searchPodcastChannels(query: String) {
        _podcastChannelQuery.value = query
        podcastChannelJob?.cancel()
        podcastChannelJob =
            viewModelScope.launch {
                _podcastChannelsLoading.value = true
                if (query.isNotBlank()) {
                    delay(350)
                }
                val cleanQuery = query.trim()
                val queryTokens = cleanQuery.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val channelMap = LinkedHashMap<String, Content>()

                _homeItemList.value.forEach { section ->
                    section.contents.filterNotNull().forEach { item ->
                        item.artists?.forEach { artist ->
                            val id = artist.id
                            val name = artist.name.trim()
                            if (!id.isNullOrBlank() &&
                                id.startsWith("UC") &&
                                name.isNotEmpty() &&
                                !isMusicChannelOrTitle(name) &&
                                (queryTokens.isEmpty() || queryTokens.all { name.contains(it, ignoreCase = true) })
                            ) {
                                channelMap[name.lowercase()] =
                                    Content(
                                        album = null,
                                        artists = listOf(Artist(id = id, name = name)),
                                        description = null,
                                        isExplicit = null,
                                        playlistId = null,
                                        browseId = id,
                                        thumbnails = item.thumbnails,
                                        title = name,
                                        videoId = null,
                                        views = null,
                                        radio = null,
                                    )
                            }
                        }
                    }
                }

                fetchChannelsForQuery(cleanQuery, channelMap, strictMatch = true)

                val sortedResults =
                    if (queryTokens.isEmpty()) {
                        channelMap.values.toList()
                    } else {
                        channelMap.values.sortedByDescending { item ->
                            val titleLower = item.title.lowercase()
                            var score = 0
                            if (titleLower == cleanQuery.lowercase()) score += 100
                            if (titleLower.contains(cleanQuery.lowercase())) score += 60
                            queryTokens.forEach { token ->
                                if (titleLower.contains(token)) score += 25
                            }
                            if (item.browseId?.startsWith("UC") == true) score += 15
                            score
                        }
                    }

                _podcastChannels.value = sortedResults
                _podcastChannelsLoading.value = false

                // Also merge newly found channels into _allPodcastChannels so directory keeps growing
                if (sortedResults.isNotEmpty()) {
                    val mergedAll = LinkedHashMap<String, Content>()
                    _allPodcastChannels.value.forEach { mergedAll[it.title.lowercase()] = it }
                    sortedResults.forEach { mergedAll[it.title.lowercase()] = it }
                    _allPodcastChannels.value = mergedAll.values.sortedBy { it.title.trim().lowercase() }
                }
            }
    }

    fun loadAllPodcastChannels(forceRefresh: Boolean = false) {
        if (_allPodcastChannels.value.size >= 40 && !forceRefresh) return
        allPodcastChannelsJob?.cancel()
        allPodcastChannelsJob =
            viewModelScope.launch {
                _allPodcastChannelsLoading.value = true
                val aggregatedMap = java.util.Collections.synchronizedMap(LinkedHashMap<String, Content>())

                // Keep existing items
                _allPodcastChannels.value.forEach { item ->
                    if (!isMusicChannelOrTitle(item.title)) {
                        aggregatedMap[item.title.lowercase()] = item
                    }
                }
                _podcastChannels.value.forEach { item ->
                    if (!isMusicChannelOrTitle(item.title)) {
                        aggregatedMap[item.title.lowercase()] = item
                    }
                }
                _homeItemList.value.forEach { section ->
                    section.contents.filterNotNull().forEach { item ->
                        item.artists?.forEach { artist ->
                            val id = artist.id
                            val name = artist.name.trim()
                            if (!id.isNullOrBlank() && id.startsWith("UC") && name.isNotEmpty() && !isMusicChannelOrTitle(name)) {
                                aggregatedMap[name.lowercase()] =
                                    Content(
                                        album = null,
                                        artists = listOf(Artist(id = id, name = name)),
                                        description = null,
                                        isExplicit = null,
                                        playlistId = null,
                                        browseId = id,
                                        thumbnails = item.thumbnails,
                                        title = name,
                                        videoId = null,
                                        views = null,
                                        radio = null,
                                    )
                            }
                        }
                    }
                }

                // Broad categories + A-Z letter coverage so every alphabet section fills up
                val discoveryQueries = buildList {
                    addAll(
                        listOf(
                            "top podcast",
                            "hindi podcast",
                            "indian podcast",
                            "english podcast",
                            "business podcast",
                            "tech podcast",
                            "spiritual podcast",
                            "storytelling podcast",
                            "comedy podcast",
                            "health science podcast",
                            "finance podcast",
                            "history podcast",
                            "self improvement podcast",
                            "crime mystery podcast",
                            "sports podcast",
                        ),
                    )
                    // Add A-Z targeted podcast queries
                    ('a'..'z').forEach { ch ->
                        add("$ch podcast")
                    }
                }

                // Execute in batches of 5 to avoid throttling while loading fast
                discoveryQueries.chunked(5).forEach { batch ->
                    batch.map { q ->
                        async {
                            val localMap = LinkedHashMap<String, Content>()
                            fetchChannelsForQuery(q, localMap, strictMatch = false)
                            localMap.forEach { (k, v) ->
                                if (!aggregatedMap.containsKey(k) || v.browseId?.startsWith("UC") == true) {
                                    aggregatedMap[k] = v
                                }
                            }
                        }
                    }.awaitAll()

                    _allPodcastChannels.value =
                        aggregatedMap.values.sortedBy { it.title.trim().lowercase() }
                }

                _allPodcastChannelsLoading.value = false
            }
    }

    fun searchInsideAllChannels(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return
        viewModelScope.launch {
            val localMap = LinkedHashMap<String, Content>()
            fetchChannelsForQuery(cleanQuery, localMap, strictMatch = true)
            if (localMap.isNotEmpty()) {
                val merged = LinkedHashMap<String, Content>()
                _allPodcastChannels.value.forEach { merged[it.title.lowercase()] = it }
                localMap.forEach { (k, v) ->
                    if (!merged.containsKey(k) || v.browseId?.startsWith("UC") == true) {
                        merged[k] = v
                    }
                }
                _allPodcastChannels.value = merged.values.sortedBy { it.title.trim().lowercase() }
            }
        }
    }

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }

    fun getHomeItemList(params: String? = null) {
        loading.value = true
        _homeListState.value = ListState.LOADING
        language =
            runBlocking {
                dataStoreManager.getString(SELECTED_LANGUAGE).first()
                    ?: SUPPORTED_LANGUAGE.codes.first()
            }
        regionCode = runBlocking { dataStoreManager.location.first() }
        homeJob?.cancel()
        homeJob =
            viewModelScope.launch {
                combine(
                    homeRepository.getHomeData(
                        params,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ),
                    homeRepository.getMoodAndMomentsData(),
                    homeRepository.getChartData(dataStoreManager.chartKey.first()),
                    homeRepository.getNewRelease(
                        getString(Res.string.new_release),
                        getString(Res.string.music_video),
                    ),
                ) { home, exploreMood, exploreChart, newRelease ->
                    HomeDataCombine(home, exploreMood, exploreChart, newRelease)
                }.collect { result ->
                    val home = result.home
                    Logger.d("home size", "${home.data?.second?.size}")
                    val exploreMoodItem = result.mood
                    val chart = result.chart
                    val newRelease = result.newRelease
                    when (home) {
                        is Resource.Success -> {
                            _continuation.value = home.data?.first
                            _homeItemList.value = home.data?.second ?: listOf()
                        }

                        else -> {
                            _continuation.value = null
                            _homeItemList.value = listOf()
                        }
                    }
                    if (continuation.value.isNullOrEmpty()) {
                        _homeListState.value = ListState.PAGINATION_EXHAUST
                    } else {
                        _homeListState.value = ListState.IDLE
                    }
                    when (chart) {
                        is Resource.Success -> {
                            _chart.value = chart.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    when (newRelease) {
                        is Resource.Success -> {
                            _newRelease.value = newRelease.data ?: arrayListOf()
                        }

                        else -> {
                            _newRelease.value = arrayListOf()
                        }
                    }
                    when (exploreMoodItem) {
                        is Resource.Success -> {
                            _exploreMoodItem.value = exploreMoodItem.data
                        }

                        else -> {
                            _exploreMoodItem.value = null
                        }
                    }
                    regionCodeChart.value = dataStoreManager.chartKey.first()
                    Logger.d("HomeViewModel", "getHomeItemList: $result")
                    dataStoreManager.cookie.first().let {
                        if (it != "") {
                            _accountInfo.emit(
                                Pair(
                                    dataStoreManager.getString("AccountName").first(),
                                    dataStoreManager.getString("AccountThumbUrl").first(),
                                ),
                            )
                        }
                    }
                    when {
                        home is Resource.Error -> home.message
                        exploreMoodItem is Resource.Error -> exploreMoodItem.message
                        chart is Resource.Error -> chart.message
                        else -> null
                    }?.let {
                        showSnackBarErrorState.emit(it)
                        Logger.w("Error", "getHomeItemList: ${home.message}")
                        Logger.w("Error", "getHomeItemList: ${exploreMoodItem.message}")
                        Logger.w("Error", "getHomeItemList: ${chart.message}")
                    }
                    loading.value = false
                }
            }
    }

    fun getContinueHomeItem(continuation: String?) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _homeListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                log("Get more home item with continuation: $continuation")
                _homeListState.value = ListState.PAGINATING
                homeRepository
                    .getHomeDataContinue(
                        continuation,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                val newItems = home.data?.second ?: listOf()
                                _homeItemList.update { it + newItems }
                                if (home.data?.first.isNullOrEmpty()) {
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value = ListState.IDLE
                                }
                            }

                            is Resource.Error -> {
                                _continuation.value = null
                                Logger.w(tag, "getContinueHomeItem: ${home.message}")
                                showSnackBarErrorState.emit(home.message ?: "Unknown error")
                                _homeListState.value = ListState.PAGINATION_EXHAUST
                            }
                        }
                    }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            homeRepository
                .getChartData(
                    region,
                ).collect { values ->
                    regionCodeChart.value = region
                    dataStoreManager.setChartKey(region)
                    when (values) {
                        is Resource.Success -> {
                            _chart.value = values.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                }
        }
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
        podcastChannelJob?.cancel()
        allPodcastChannelsJob?.cancel()
    }

    companion object {
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
        const val HOME_PARAMS_PODCASTS = "FEmusic_podcasts"
    }
}