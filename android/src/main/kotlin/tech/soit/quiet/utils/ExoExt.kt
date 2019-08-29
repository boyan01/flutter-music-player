package tech.soit.quiet.utils

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource

/**
 * Extension method for building a [ConcatenatingMediaSource] given a [List]
 * of [MediaDescriptionCompat] objects.
 */
fun List<MediaDescriptionCompat>.toMediaSource(
    dataSourceFactory: DataSource.Factory
): ConcatenatingMediaSource {

    val concatenatingMediaSource = ConcatenatingMediaSource()
    forEach {
        concatenatingMediaSource.addMediaSource(it.toMediaSource(dataSourceFactory))
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
        .createMediaSource(mediaUri)