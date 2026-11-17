package com.maxrave.data.repository

import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.data.extension.getFullDataFromDB
import com.maxrave.data.parser.parsePodcastContinueData
import com.maxrave.data.parser.parsePodcastData
import com.maxrave.data.parser.toListThumbnail
import com.maxrave.domain.data.entities.EpisodeEntity
import com.maxrave.domain.data.entities.PodcastWithEpisodes
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.model.podcast.PodcastBrowse
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class PodcastRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : PodcastRepository {
    override fun getPodcastData(podcastId: String): Flow<Resource<PodcastBrowse>> =
        flow {
            runCatching {
                youTube
                    .customQuery(browseId = podcastId)
                    .onSuccess { result ->
                        val listEpisode = arrayListOf<PodcastBrowse.EpisodeItem>()
                        val thumbnail =
                            result.background
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.toListThumbnail()
                        val headerRenderer =
                            result.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                        val title =
                            headerRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                        val author =
                            headerRenderer?.let {
                                Artist(
                                    id =
                                        it.straplineTextOne
                                            ?.runs
                                            ?.firstOrNull()
                                            ?.navigationEndpoint
                                            ?.browseEndpoint
                                            ?.browseId,
                                    name =
                                        it.straplineTextOne
                                            ?.runs
                                            ?.firstOrNull()
                                            ?.text ?: "",
                                )
                            }
                        val authorThumbnail =
                            headerRenderer
                                ?.straplineThumbnail
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url
                        val description =
                            headerRenderer
                                ?.description
                                ?.musicDescriptionShelfRenderer
                                ?.description
                                ?.runs
                                ?.joinToString("") {
                                    it.text
                                }
                        val shelfRenderer =
                            result.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.secondaryContents
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                        parsePodcastData(shelfRenderer?.contents, author).let {
                            listEpisode.addAll(it)
                        }

                        if (author != null) {
                            // Pehla batch aate hi turant screen open kar dein!
                            emit(
                                Resource.Success(
                                    PodcastBrowse(
                                        title = title ?: "",
                                        author = author,
                                        authorThumbnail = authorThumbnail,
                                        thumbnail = thumbnail ?: emptyList<Thumbnail>(),
                                        description = description,
                                        listEpisode = listEpisode.toList(),
                                    ),
                                ),
                            )

                            // Baaki episodes background mein load karke update karein
                            var continueParam =
                                shelfRenderer
                                    ?.continuations
                                    ?.firstOrNull()
                                    ?.nextContinuationData
                                    ?.continuation
                            while (continueParam != null) {
                                youTube
                                    .customQuery(continuation = continueParam, browseId = "")
                                    .onSuccess { continueData ->
                                        val moreEpisodes =
                                            parsePodcastContinueData(
                                                continueData.continuationContents?.musicShelfContinuation?.contents,
                                                author,
                                            )
                                        if (moreEpisodes.isNotEmpty()) {
                                            listEpisode.addAll(moreEpisodes)
                                            emit(
                                                Resource.Success(
                                                    PodcastBrowse(
                                                        title = title ?: "",
                                                        author = author,
                                                        authorThumbnail = authorThumbnail,
                                                        thumbnail = thumbnail ?: emptyList<Thumbnail>(),
                                                        description = description,
                                                        listEpisode = listEpisode.toList(),
                                                    ),
                                                ),
                                            )
                                        }
                                        continueParam =
                                            continueData.continuationContents
                                                ?.musicShelfContinuation
                                                ?.continuations
                                                ?.firstOrNull()
                                                ?.nextContinuationData
                                                ?.continuation
                                    }.onFailure {
                                        it.printStackTrace()
                                        continueParam = null
                                    }
                            }
                        } else {
                            emit(Resource.Error<PodcastBrowse>("Error"))
                        }
                    }.onFailure { error ->
                        Logger.w("Podcast", "Error: ${error.message}")
                        emit(Resource.Error<PodcastBrowse>(error.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun insertPodcast(podcastsEntity: PodcastsEntity) =
        flow {
            emit(localDataSource.insertPodcast(podcastsEntity))
        }.flowOn(Dispatchers.IO)

    override fun insertEpisodes(episodes: List<EpisodeEntity>) =
        flow {
            emit(localDataSource.insertEpisodes(episodes))
        }.flowOn(Dispatchers.IO)

    override fun getPodcastWithEpisodes(podcastId: String): Flow<PodcastWithEpisodes?> =
        flow {
            emit(localDataSource.getPodcastWithEpisodes(podcastId))
        }.flowOn(Dispatchers.IO)

    override fun getAllPodcasts(limit: Int): Flow<List<PodcastsEntity>> =
        flow {
            emit(localDataSource.getAllPodcasts(limit))
        }.flowOn(Dispatchers.IO)

    override fun getAllPodcastWithEpisodes(): Flow<List<PodcastWithEpisodes>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getAllPodcastWithEpisodes(limit, offset)
                }
            )
        }.flowOn(Dispatchers.IO)

    override fun getPodcast(podcastId: String): Flow<PodcastsEntity?> =
        flow {
            emit(localDataSource.getPodcast(podcastId))
        }.flowOn(Dispatchers.IO)

    override fun getEpisode(videoId: String): Flow<EpisodeEntity?> =
        flow {
            emit(localDataSource.getEpisode(videoId))
        }.flowOn(Dispatchers.IO)

    override fun deletePodcast(podcastId: String) =
        flow {
            emit(localDataSource.deletePodcast(podcastId))
        }.flowOn(Dispatchers.IO)

    override fun favoritePodcast(
        podcastId: String,
        favorite: Boolean,
    ) = flow {
        emit(localDataSource.favoritePodcast(podcastId, favorite))
    }.flowOn(Dispatchers.IO)

    override fun getPodcastEpisodes(podcastId: String): Flow<List<EpisodeEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getPodcastEpisodes(podcastId, limit, offset)
                }
            )
        }.flowOn(Dispatchers.IO)

    override fun getFavoritePodcasts(): Flow<List<PodcastsEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getFavoritePodcasts(limit, offset)
                }
            )
        }.flowOn(Dispatchers.IO)

    override fun updatePodcastInLibraryNow(id: String) =
        flow {
            emit(localDataSource.updatePodcastInLibraryNow(id))
        }.flowOn(Dispatchers.IO)
}