package tech.soit.quiet.utils

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.runBlocking
import tech.soit.quiet.BackgroundHandle
import tech.soit.quiet.service.MusicPlayerService

/**
 * Extension method for building a [ConcatenatingMediaSource] given a [List]
 * of [MediaDescriptionCompat] objects.
 */
fun List<MediaDescriptionCompat>.toMediaSource(
    service: MusicPlayerService
): ConcatenatingMediaSource {
    val handle = service.backgroundHandle
    val default = DefaultDataSourceFactory(
        service,
        handle.config.userAgent ?: Util.getUserAgent(service, service.applicationInfo.name),
        null
    )
    var factory: DataSource.Factory =
        UrlUpdatingDataSource.Factory(default, handle)
    if (handle.config.enableCache) {
        factory = CacheDataSourceFactory(SimpleCache(service.cacheDir, NoOpCacheEvictor()), factory)
    }

    val concatenatingMediaSource = ConcatenatingMediaSource(
        true, true,
        ShuffleOrder.DefaultShuffleOrder(0)
    )
    forEach {
        concatenatingMediaSource.addMediaSource(it.toMediaSource(factory))
    }
    return concatenatingMediaSource
}

/**
 * Extension method for building an [ExtractorMediaSource] from a [MediaMetadataCompat] object.
 *
 * For convenience, place the [MediaDescriptionCompat] into the tag so it can be retrieved later.
 */
fun MediaDescriptionCompat.toMediaSource(dataSourceFactory: DataSource.Factory): ExtractorMediaSource =
    ExtractorMediaSource.Factory(dataSourceFactory)
        .setTag(this)
        .setCustomCacheKey(mediaId)
        .createMediaSource(buildMediaUri(this))


private const val SCHEME = "quiet"

/**
 * Build a Decorated Uri for this Media.
 * this uri will handle in [UrlUpdatingDataSource]
 */
private fun buildMediaUri(description: MediaDescriptionCompat): Uri {
    return Uri.parse(
        "$SCHEME://player?" +
                "id=${description.mediaId}&" +
                "uri=${description.mediaUri?.toString() ?: ""}"
    )
}


/**
 * auto update url when we read data from dataSource
 */
private class UrlUpdatingDataSource(
    private val dataSource: DataSource,
    private val backgroundHandle: BackgroundHandle
) : DataSource by dataSource {

    class Factory(
        private val factory: DataSource.Factory,
        private val backgroundHandle: BackgroundHandle
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return UrlUpdatingDataSource(
                factory.createDataSource(),
                backgroundHandle
            )
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        val former = dataSpec.uri
        return if (former.scheme == SCHEME) {
            val id = former.getQueryParameter("id")!!
            val fallback = former.getQueryParameter("uri")
            val uri = runBlocking {
                backgroundHandle.getPlayUrl(id, fallback)
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