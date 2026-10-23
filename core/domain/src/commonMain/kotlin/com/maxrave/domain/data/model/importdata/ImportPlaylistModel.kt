package com.maxrave.domain.data.model.importdata

enum class SupportedPlatform {
    SPOTIFY,
    APPLE_MUSIC,
    UNKNOWN
}

data class ExternalTrackInfo(
    val title: String,
    val artist: String,
    val durationSeconds: Long = 0,
)

data class ExternalPlaylistInfo(
    val name: String,
    val description: String = "",
    val platform: SupportedPlatform,
    val tracks: List<ExternalTrackInfo>,
)

object PlaylistLinkHelper {
    fun detectPlatform(url: String): SupportedPlatform =
        when {
            url.contains("spotify.com") -> SupportedPlatform.SPOTIFY
            url.contains("music.apple.com") -> SupportedPlatform.APPLE_MUSIC
            else -> SupportedPlatform.UNKNOWN
        }

    fun extractSpotifyPlaylistId(url: String): String? {
        val pattern = Regex("playlist/([a-zA-Z0-9]+)")
        return pattern.find(url)?.groupValues?.get(1)
    }

    fun extractAppleMusicPlaylistId(url: String): String? {
        val pattern = Regex("playlist/[^/]+/(pl\\.[a-zA-Z0-9]+)")
        return pattern.find(url)?.groupValues?.get(1)
    }
}