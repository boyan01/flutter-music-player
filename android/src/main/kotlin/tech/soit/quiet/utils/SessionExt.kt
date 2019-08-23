package tech.soit.quiet.utils

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat


/**
 * helper to convert a PlaybackState to map
 */
fun PlaybackStateCompat.toMap(): MutableMap<String, *> {
    return mutableMapOf<String, Any>(
            "state" to state,
            "position" to position,
            "playbackSpeed" to playbackSpeed,
            "lastPositionUpdateTime" to lastPositionUpdateTime,
            "bufferedPosition" to bufferedPosition,
            "actions" to actions,
            "errorMessage" to errorMessage,
            "activeQueueItemId" to activeQueueItemId,
            "errorCode" to errorCode
    )
}


fun Map<*, *>.toMediaMetadataCompat(): MediaDescriptionCompat {
    return MediaDescriptionCompat.Builder()
            .setMediaId(get("id").toString())
            .setTitle(get("title") as CharSequence?)
            .setSubtitle(get("subtitle") as CharSequence?)
            .setMediaUri((get("url") as String?)?.let { Uri.parse(it) })
            .setExtras(toBundle())
            .build()
}

fun MediaMetadataCompat?.toMap(): Map<String, *>? {
    return this?.bundle?.toMap()
}

fun MediaDescriptionCompat.toMap(): Map<String, *> {
    return extras!!.toMap()
}

fun MediaDescriptionCompat.toMediaItem(): MediaBrowserCompat.MediaItem {
    return MediaBrowserCompat.MediaItem(this, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
}

private fun Bundle.toMap(): Map<String, Any?> {
    return keySet().map {
        val obj = get(it)
        if (obj is Bundle) {
            it to obj.toMap()
        } else {
            it to get(it)
        }
    }.toMap()
}


private fun Map<*, *>.toBundle(): Bundle {
    val bundle = Bundle()
    forEach { entry ->
        val key = entry.key
        val value = entry.value
        if (key !is String?) return@forEach
        when (value) {
            is String -> bundle.putString(key, value)
            is Map<*, *> -> bundle.putBundle(key, value.toBundle())
        }
    }
    return bundle
}