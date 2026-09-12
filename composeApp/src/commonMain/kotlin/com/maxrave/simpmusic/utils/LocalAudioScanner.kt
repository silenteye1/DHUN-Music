package com.maxrave.simpmusic.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.maxrave.domain.data.entities.SongEntity

object LocalAudioScanner {
    fun getLocalAudioTracks(context: Context): List<SongEntity> {
        val songs = mutableListOf<SongEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val rawTitle = cursor.getString(titleColumn)
                val rawArtist = cursor.getString(artistColumn)
                val durationMs = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val rawAlbum = cursor.getString(albumColumn)

                val title = if (rawTitle.isNullOrBlank()) "Unknown Title" else rawTitle
                val artist = if (rawArtist.isNullOrBlank()) "Unknown Artist" else rawArtist
                val album = if (rawAlbum.isNullOrBlank()) "Unknown Album" else rawAlbum

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                val totalSeconds = (durationMs / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val formattedDuration = "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"

                songs.add(
                    SongEntity(
                        videoId = contentUri.toString(),
                        title = title,
                        albumId = albumId.toString(),
                        albumName = album,
                        artistId = listOf(artist),
                        artistName = listOf(artist),
                        duration = formattedDuration,
                        durationSeconds = totalSeconds,
                        isAvailable = true,
                        isExplicit = false,
                        likeStatus = "INDIFFERENT",
                        thumbnails = albumArtUri,
                        videoType = "video",
                        category = "Local",
                        resultType = "song",
                        liked = false,
                        totalPlayTime = 0L,
                        downloadState = 3 // STATE_DOWNLOADED (Direct local storage playback bina YouTube API call ke)
                    )
                )
            }
        }
        return songs
    }
}