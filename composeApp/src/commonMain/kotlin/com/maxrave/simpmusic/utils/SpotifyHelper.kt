package com.maxrave.simpmusic.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object SpotifyHelper {

    fun isSpotifyUrl(url: String): Boolean {
        return url.contains("spotify.com") || url.contains("spotify.link")
    }

    fun isPlaylistUrl(url: String): Boolean {
        return url.contains("/playlist/") || url.contains("/album/")
    }

    fun extractUrl(text: String): String? {
        val regex = "(https?://[a-zA-Z0-9./?=_-]+)".toRegex()
        val matches = regex.findAll(text)
        return matches.firstOrNull { isSpotifyUrl(it.value) }?.value
    }

    private fun resolveRedirect(urlStr: String): String {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val redirect = conn.getHeaderField("Location")
            if (!redirect.isNullOrBlank()) redirect else urlStr
        } catch (e: Exception) {
            urlStr
        }
    }

    suspend fun getSearchQueryFromSpotifyUrl(spotifyUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val finalUrl = if (spotifyUrl.contains("spotify.link")) resolveRedirect(spotifyUrl) else spotifyUrl
            val encodedUrl = java.net.URLEncoder.encode(finalUrl, "UTF-8")
            val endpoint = "https://open.spotify.com/oembed?url=$encodedUrl"
            val responseText = fetchHttp(endpoint) ?: return@withContext null
            val json = JSONObject(responseText)
            val title = json.optString("title", "")
            if (title.isNotBlank()) title else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTracksFromSpotifyPlaylist(playlistUrl: String): List<String> = withContext(Dispatchers.IO) {
        val tracksList = mutableListOf<String>()
        try {
            val finalUrl = if (playlistUrl.contains("spotify.link")) resolveRedirect(playlistUrl) else playlistUrl
            val cleanUrl = finalUrl.substringBefore("?")
            val embedUrl = cleanUrl.replace("open.spotify.com/", "open.spotify.com/embed/")

            val html = fetchHttp(embedUrl) ?: return@withContext emptyList()

            // 1. First attempt: Read standard trackList from next_data
            val pattern = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val jsonString = matcher.group(1) ?: ""
                val rootJson = JSONObject(jsonString)
                val entityState = rootJson
                    .getJSONObject("props")
                    .getJSONObject("pageProps")
                    .getJSONObject("state")
                    .getJSONObject("data")
                    .getJSONObject("entity")

                val trackListArray = entityState.optJSONObject("trackList")?.optJSONArray("items")
                    ?: entityState.optJSONArray("trackList")

                if (trackListArray != null) {
                    for (i in 0 until trackListArray.length()) {
                        val item = trackListArray.getJSONObject(i)
                        val title = item.optString("title", item.optString("name", ""))
                        val subtitle = item.optString("subtitle", "")
                        if (title.isNotBlank()) {
                            val query = if (subtitle.isNotBlank()) "$title $subtitle" else title
                            tracksList.add(query.trim())
                        }
                    }
                }
            }

            // 2. Fallback regex match if JSON format changed
            if (tracksList.isEmpty()) {
                val titleRegex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val artistRegex = "\"artists\"\\s*:\\s*\\[\\{\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()

                val titles = titleRegex.findAll(html).map { it.groupValues[1] }.toList()
                val artists = artistRegex.findAll(html).map { it.groupValues[1] }.toList()

                for (i in titles.indices) {
                    val t = titles[i]
                    val a = artists.getOrNull(i) ?: ""
                    if (!t.contains("Spotify", true) && t.isNotBlank()) {
                        tracksList.add("$t $a".trim())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracksList.distinct()
    }

    private fun fetchHttp(urlStr: String): String? {
        return try {
            val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}