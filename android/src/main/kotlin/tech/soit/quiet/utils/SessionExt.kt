package tech.soit.quiet.utils

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


fun Map<*, *>.toMediaMetadataCompat(): MediaMetadataCompat {
    //TODO handle more
    return MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, get("mediaId").toString())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, get("title").toString())
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, get("subtitle").toString())
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, get("mediaUri").toString())
        .build()
}

fun MediaMetadataCompat?.toMap(): Map<String, *>? {
    if (this == null) return null
    return mapOf(
        "title" to getString(MediaMetadataCompat.METADATA_KEY_TITLE),
        "artist" to getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
        "duration" to getLong(MediaMetadataCompat.METADATA_KEY_DURATION),
        "album" to getString(MediaMetadataCompat.METADATA_KEY_ALBUM),
        "writer" to getString(MediaMetadataCompat.METADATA_KEY_WRITER),
        "composer" to getString(MediaMetadataCompat.METADATA_KEY_COMPOSER),
        "compilation" to getString(MediaMetadataCompat.METADATA_KEY_COMPILATION),
        "date" to getString(MediaMetadataCompat.METADATA_KEY_DATE),
        "year" to getLong(MediaMetadataCompat.METADATA_KEY_YEAR),
        "genre" to getString(MediaMetadataCompat.METADATA_KEY_GENRE),
        "trackNumber" to getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER),
        "numTracks" to getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS),
        "discNumber" to getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER),
        "albumArtist" to getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST),
//    "art" tox getBitmap(MediaMetadataCompat.METADATA_KEY_ART),
        "artUri" to getString(MediaMetadataCompat.METADATA_KEY_ART_URI),
//    "albumArt" to getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART),
        "albumArtUri" to getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI),
//    "userRating" to getString(MediaMetadataCompat.METADATA_KEY_USER_RATING),
//    "rating" to getString(MediaMetadataCompat.METADATA_KEY_RATING),
        "displayTitle" to getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE),
        "displaySubtitle" to getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE),
        "displayDescription" to getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION),
//    "displayIcon" to getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON),
        "displayIconUri" to getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI),
        "mediaId" to getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
        "btFolderType" to getLong(MediaMetadataCompat.METADATA_KEY_BT_FOLDER_TYPE),
        "mediaUri" to getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI),
        "advertisement" to getLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT),
        "downloadStatus" to getLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS)
    )
}

fun MediaDescriptionCompat.toMap(): Map<String, *> {
    return mapOf(
        "title" to title,
        "mediaId" to mediaId,
        "subtitle" to subtitle,
        "description" to description,
        "iconUri" to iconUri?.toString(),
        "extras" to extras?.toMap()
    )
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