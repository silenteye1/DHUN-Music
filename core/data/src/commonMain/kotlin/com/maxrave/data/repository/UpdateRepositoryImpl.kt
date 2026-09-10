package com.maxrave.data.repository

import com.maxrave.domain.data.model.update.UpdateData
import com.maxrave.domain.repository.UpdateRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

internal class UpdateRepositoryImpl(
    private val youTube: YouTube,
) : UpdateRepository {

    private val repoOwner = "silenteye1"
    private val repoName = "DHUN-Music"

    override fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>> =
        flow {
            try {
                val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
                val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = Json.parseToJsonElement(responseText).jsonObject
                    val tagName = json["tag_name"]?.jsonPrimitive?.content ?: ""
                    val publishedAt = json["published_at"]?.jsonPrimitive?.content ?: ""
                    val body = json["body"]?.jsonPrimitive?.content ?: "Bug fixes and improvements"

                    emit(
                        Resource.Success(
                            UpdateData(
                                tagName = tagName,
                                releaseTime = publishedAt,
                                body = body,
                            ),
                        ),
                    )
                } else {
                    emit(Resource.Error<UpdateData>("GitHub release check failed with HTTP ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                emit(Resource.Error<UpdateData>(e.localizedMessage ?: "Network error checking updates"))
            }
        }.flowOn(Dispatchers.IO)

    override fun checkForFdroidUpdate(): Flow<Resource<UpdateData>> =
        flow {
            emit(Resource.Error<UpdateData>("F-Droid channel is currently disabled. Please use GitHub channel."))
        }.flowOn(Dispatchers.IO)
}