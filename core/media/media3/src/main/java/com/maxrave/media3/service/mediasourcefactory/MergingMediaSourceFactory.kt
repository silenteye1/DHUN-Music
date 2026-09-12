package com.maxrave.media3.service.mediasourcefactory

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform.getKoin

@UnstableApi
internal class MergingMediaSourceFactory(
    private val defaultMediaSourceFactory: DefaultMediaSourceFactory,
    private val dataStoreManager: DataStoreManager,
) : MediaSource.Factory {

    // Device storage ke gaano ke liye plain Android media source factory
    private val standardMediaSourceFactory by lazy {
        val context: Context = getKoin().get()
        DefaultMediaSourceFactory(context)
    }

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        defaultMediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        defaultMediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        return this
    }

    override fun getSupportedTypes(): IntArray = defaultMediaSourceFactory.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val uriString = mediaItem.requestMetadata.mediaUri?.toString()
            ?: mediaItem.localConfiguration?.uri?.toString()
            ?: mediaItem.mediaId

        // Agar gaana device storage ka hai, toh direct play karein (online resolver bypass)
        if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
            Logger.d("Merging Media Source", "Playing local audio directly: $uriString")
            return standardMediaSourceFactory.createMediaSource(mediaItem)
        }

        Logger.w("Merging Media Source", mediaItem.mediaMetadata.description.toString())
        val getVideo = runBlocking(Dispatchers.IO) { dataStoreManager.watchVideoInsteadOfPlayingAudio.first() } == DataStoreManager.Values.TRUE
        Logger.w("Merging Media Source", getVideo.toString())
        if (mediaItem.mediaMetadata.description == MERGING_DATA_TYPE.VIDEO && getVideo) {
            val videoItem =
                mediaItem
                    .buildUpon()
                    .setMediaId("${MERGING_DATA_TYPE.VIDEO}${mediaItem.mediaId}")
                    .setCustomCacheKey("${MERGING_DATA_TYPE.VIDEO}${mediaItem.mediaId}")
                    .build()
            return MergingMediaSource(
                defaultMediaSourceFactory.createMediaSource(videoItem),
                defaultMediaSourceFactory.createMediaSource(mediaItem),
            )
        } else {
            return defaultMediaSourceFactory.createMediaSource(mediaItem)
        }
    }
}