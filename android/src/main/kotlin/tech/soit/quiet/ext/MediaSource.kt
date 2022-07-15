package tech.soit.quiet.ext

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import kotlinx.coroutines.runBlocking
import tech.soit.quiet.MusicPlayerServicePlugin
import tech.soit.quiet.player.MusicMetadata

internal fun MusicMetadata.toMediaSource(
    context: Context,
    servicePlugin: MusicPlayerServicePlugin
): ProgressiveMediaSource {
    var factory: DataSource.Factory = DefaultDataSource.Factory(context)
    factory = UrlUpdatingDataSource.Factory(factory, servicePlugin)
    if (servicePlugin.config.enableCache) {
        factory = CacheDataSourceFactory(SimpleCache(context.cacheDir, NoOpCacheEvictor()), factory)
    }
    return ProgressiveMediaSource.Factory(factory)
        .setCustomCacheKey(mediaId)
        .createMediaSource(buildMediaUri(this))
}


private const val SCHEME = "quiet"

/**
 * Build a Decorated Uri for this Media.
 * this uri will handle in [UrlUpdatingDataSource]
 */
private fun buildMediaUri(metadata: MusicMetadata): Uri {
    return Uri.Builder()
        .scheme(SCHEME)
        .path("player")
        .appendQueryParameter("id", metadata.mediaId)
        .appendQueryParameter("uri", metadata.mediaUri ?: "")
        .build()
}


/**
 * auto update url when we read data from dataSource
 */
class UrlUpdatingDataSource(
    private val dataSource: DataSource,
    private val backgroundChannel: MusicPlayerServicePlugin
) : DataSource by dataSource {

    class Factory(
        private val factory: DataSource.Factory,
        private val backgroundChannel: MusicPlayerServicePlugin
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return UrlUpdatingDataSource(
                factory.createDataSource(),
                backgroundChannel
            )
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        val former = dataSpec.uri
        return if (former.scheme == SCHEME) {
            val id = former.getQueryParameter("id")!!
            val fallback = former.getQueryParameter("uri")
            val uri = runBlocking {
                val playUrl = backgroundChannel.getPlayUrl(id, fallback)
                if ("asset".equals(playUrl.scheme, true)) {
                    return@runBlocking Uri.parse("asset:///flutter_assets${playUrl.path}")
                } else {
                    playUrl
                }
            }
            val newSpec = dataSpec.withUri(uri)
            dataSource.open(newSpec)
        } else {
            dataSource.open(dataSpec)
        }
    }


    override fun getUri(): Uri? {
        return dataSource.uri
    }


    override fun close() {
        dataSource.close()
    }

}
