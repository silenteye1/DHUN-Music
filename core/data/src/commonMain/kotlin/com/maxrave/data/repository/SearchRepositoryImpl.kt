package com.maxrave.data.repository

import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.data.mapping.toDomainSearchSuggestions
import com.maxrave.data.parser.parsePodcast
import com.maxrave.data.parser.search.parseSearchAlbum
import com.maxrave.data.parser.search.parseSearchArtist
import com.maxrave.data.parser.search.parseSearchPlaylist
import com.maxrave.data.parser.search.parseSearchSong
import com.maxrave.data.parser.search.parseSearchVideo
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.model.searchResult.SearchSuggestions
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class SearchRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : SearchRepository {
    override fun getSearchHistory(): Flow<List<SearchHistory>> =
        flow {
            emit(localDataSource.getSearchHistory())
        }.flowOn(Dispatchers.IO)

    override fun insertSearchHistory(searchHistory: SearchHistory): Flow<Long> =
        flow {
            emit(localDataSource.insertSearchHistory(searchHistory))
        }.flowOn(Dispatchers.IO)

    override suspend fun deleteSearchHistory() =
        withContext(Dispatchers.IO) {
            localDataSource.deleteSearchHistory()
        }

    override fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>> =
        flow {
            runCatching {
                val cleanQuery = query.trim()
                val isFreshness = cleanQuery.contains("new", ignoreCase = true) ||
                    cleanQuery.contains("latest", ignoreCase = true) ||
                    cleanQuery.contains("recent", ignoreCase = true)

                // Agar query me 'new' ya 'latest' hai, toh backend se targeted candidates laane ke liye current year tag lagana
                val targetQuery = if (isFreshness && !cleanQuery.contains("2026") && !cleanQuery.contains("2025")) {
                    "$cleanQuery 2026"
                } else {
                    cleanQuery
                }

                youTube
                    .search(targetQuery, YouTube.SearchFilter.FILTER_SONG)
                    .onSuccess { result ->
                        val listSongs: ArrayList<SongsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchSong(result).let { list ->
                            listSongs.addAll(list)
                        }
                        var count = 0
                        // Freshness intent ke liye 3 pages tak scan taaki naye gaane missing na hon
                        val maxContinuation = if (isFreshness) 3 else 2
                        while (count < maxContinuation && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchSong(values).let { list ->
                                        listSongs.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }

                        val processedSongs = filterAndRankSongs(query, listSongs)
                        emit(Resource.Success<ArrayList<SongsResult>>(ArrayList(processedSongs)))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<SongsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_VIDEO)
                    .onSuccess { result ->
                        val listSongs: ArrayList<VideosResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchVideo(result).let { list ->
                            listSongs.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchVideo(values).let { list ->
                                        listSongs.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }

                        emit(Resource.Success<ArrayList<VideosResult>>(listSongs))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<VideosResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataPodcast(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_PODCAST)
                    .onSuccess { result ->
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        Logger.w("Podcast", "result: $result")
                        parsePodcast(result.listPodcast).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parsePodcast(values.listPodcast).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataFeaturedPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .onSuccess { result ->
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchPlaylist(result).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchPlaylist(values).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_ARTIST)
                    .onSuccess { result ->
                        val listArtist: ArrayList<ArtistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchArtist(result).let { list ->
                            listArtist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchArtist(values).let { list ->
                                        listArtist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<ArtistsResult>>(listArtist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<ArtistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_ALBUM)
                    .onSuccess { result ->
                        val listAlbum: ArrayList<AlbumsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchAlbum(result).let { list ->
                            listAlbum.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchAlbum(values).let { list ->
                                        listAlbum.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<AlbumsResult>>(listAlbum))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<AlbumsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                    .onSuccess { result ->
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchPlaylist(result).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchPlaylist(values).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>> =
        flow {
            runCatching {
                youTube
                    .getYTMusicSearchSuggestions(query)
                    .onSuccess {
                        emit(Resource.Success(it.toDomainSearchSuggestions()))
                    }.onFailure { e ->
                        Logger.d("Suggest", "Error: ${e.message}")
                        emit(Resource.Error<SearchSuggestions>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Universal Smart Search Filter & Relevance Ranker
     */
    private fun filterAndRankSongs(
        query: String,
        songs: List<SongsResult>,
    ): List<SongsResult> {
        if (songs.isEmpty()) return emptyList()

        val cleanQuery = query.trim().lowercase()
        val queryTokens = cleanQuery.split("\\s+".toRegex()).filter { it.length > 1 }

        val isFreshnessIntent = cleanQuery.contains("new") ||
            cleanQuery.contains("latest") ||
            cleanQuery.contains("recent")

        val effectiveTokens = if (isFreshnessIntent) {
            queryTokens.filter { it != "new" && it != "latest" && it != "recent" && it != "song" && it != "songs" }
        } else {
            queryTokens
        }

        // 1. Blacklist Junk Filters (Shorts, Reels, Status videos, fan-edits, spam)
        val junkKeywords = listOf(
            "whatsapp status", "status video", "fullscreen status",
            "shorts", "tiktok", "reels", "ringtone", "callertune",
            "slowed reverb", "slowed+reverb", "8d audio", "bass boosted remix",
            "30 sec", "30sec", "instrumental cover karaoke"
        )

        val cleanList = songs.filterNot { song ->
            val titleLower = song.title?.lowercase().orEmpty()
            junkKeywords.any { junk -> titleLower.contains(junk) }
        }

        // Deduplication (Title + primary artist base name)
        val seenSignatures = HashSet<String>()
        val deduplicatedList = mutableListOf<SongsResult>()

        for (song in cleanList) {
            val titleStr = song.title.orEmpty()
            val normalizedTitle = titleStr.lowercase()
                .replace("\\(.*?\\)|\\[.*?\\]".toRegex(), "")
                .replace("[^a-zA-Z0-9]".toRegex(), "")
            val primaryArtist = song.artists?.firstOrNull()?.name?.lowercase()?.replace("[^a-zA-Z0-9]".toRegex(), "").orEmpty()
            val signature = "$normalizedTitle-$primaryArtist"

            if (seenSignatures.add(signature)) {
                deduplicatedList.add(song)
            }
        }

        // 2. Score Calculation & Sorting
        val scoredList = deduplicatedList.map { song ->
            val titleLower = song.title?.lowercase().orEmpty()
            val artistsLower = song.artists?.joinToString(" ") { it.name.lowercase() }.orEmpty()
            val albumLower = song.album?.name?.lowercase().orEmpty()

            // Numerical year extraction from song.year or title
            val rawYear = song.year.toString()
            val yearMatch = "\\b(20[1-2][0-9])\\b".toRegex().find(rawYear)?.value
                ?: "\\b(20[1-2][0-9])\\b".toRegex().find(titleLower)?.value
            val parsedYear = yearMatch?.toIntOrNull() ?: 0

            var score = 0

            // Exact Query Match
            if (titleLower == cleanQuery) score += 100
            if (artistsLower.contains(cleanQuery)) score += 120

            // Relevance Token Matches
            for (token in effectiveTokens) {
                if (titleLower.contains(token)) score += 40
                if (artistsLower.contains(token)) score += 40
                if (albumLower.contains(token)) score += 15
            }

            // Official Music / Topic verification bonus
            val isOfficialChannel = song.artists?.any {
                it.name.contains("Topic", ignoreCase = true) ||
                    it.name.contains("T-Series", ignoreCase = true) ||
                    it.name.contains("Zee Music", ignoreCase = true) ||
                    it.name.contains("Sony Music", ignoreCase = true) ||
                    it.name.contains("Speed Records", ignoreCase = true) ||
                    it.name.contains("YRF", ignoreCase = true)
            } ?: false

            if (isOfficialChannel) score += 35

            // Freshness Intent Ranking
            if (isFreshnessIntent) {
                when {
                    parsedYear >= 2026 -> score += 300 // Maximum priority for latest release
                    parsedYear == 2025 -> score += 200
                    parsedYear == 2024 -> score += 100
                    parsedYear in 2020..2023 -> score += 20
                    parsedYear in 1900..2019 -> score -= 150 // Demote older tracks for 'new/latest' queries
                }

                if (titleLower.contains("2026") || titleLower.contains("latest")) {
                    score += 150
                }
            }

            song to score
        }

        return scoredList.sortedByDescending { it.second }.map { it.first }
    }
}