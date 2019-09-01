package tech.soit.quiet.utils

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
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


private val metadataKeyMapping = mapOf(
    MediaMetadataCompat.METADATA_KEY_TITLE to "title",
    MediaMetadataCompat.METADATA_KEY_ARTIST to "artist",
    MediaMetadataCompat.METADATA_KEY_DURATION to "duration",
    MediaMetadataCompat.METADATA_KEY_ALBUM to "album",
    MediaMetadataCompat.METADATA_KEY_COMPOSER to "composer",
    MediaMetadataCompat.METADATA_KEY_COMPILATION to "compilation",
    MediaMetadataCompat.METADATA_KEY_DATE to "date",
    MediaMetadataCompat.METADATA_KEY_YEAR to "year",
    MediaMetadataCompat.METADATA_KEY_GENRE to "genre",
    MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER to "trackNumber",
    MediaMetadataCompat.METADATA_KEY_NUM_TRACKS to "numTracks",
    MediaMetadataCompat.METADATA_KEY_DISC_NUMBER to "discNumber",
    MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST to "albumArtist",
    MediaMetadataCompat.METADATA_KEY_ART_URI to "artUri",
    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI to "albumArtUri",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE to "displayTitle",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE to "displaySubtitle",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION to "displayDescription",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI to "displayIconUri",
    MediaMetadataCompat.METADATA_KEY_MEDIA_ID to "mediaId",
    MediaMetadataCompat.METADATA_KEY_BT_FOLDER_TYPE to "btFolderType",
    MediaMetadataCompat.METADATA_KEY_MEDIA_URI to "mediaUri",
    MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT to "advertisement",
    // Rating
    MediaMetadataCompat.METADATA_KEY_RATING to "rating",
    MediaMetadataCompat.METADATA_KEY_USER_RATING to "userRating"
)

private val metadataKeyMappingReverse = HashMap<String, String>()
    .also {
        for ((key, value) in metadataKeyMapping) {
            it[value] = key
        }
    }.toMap()


/**
 * Convert a map to MediaMetadata, but skip Bitmap field.
 */
fun Map<*, *>.toMediaMetadataCompat(): MediaMetadataCompat = MediaMetadataCompat.Builder().also {
    for ((key, value) in this) {
        it.put(metadataKeyMappingReverse[key] ?: key, value)
    }
}.build()


private fun MediaMetadataCompat.Builder.put(key: Any?, value: Any?): MediaMetadataCompat.Builder {
    if (key !is String) return this
    value ?: return this
    when (value) {
        is String -> putString(key, value)
        is Number -> putLong(key, value.toLong())
        //Skip Bitmap/Other...
    }
    if ("rating" == key || "userRating" == key) {
        putRating(key, (value as? Map<*, *>).toRating())
    }
    return this
}

fun MediaMetadataCompat?.toMap(): Map<String, *>? {
    if (this == null) return null

    val bundle = bundle

    fun convector(any: Any?): Any? {
        any ?: return null
        return when (any) {
            is String, is Number -> any
            is RatingCompat -> any.toMap()
            else -> null
        }
    }
    return bundle.keySet().map { it to (metadataKeyMapping[it] ?: it) }
        .map { (nativeKey, dartKey) -> dartKey to convector(bundle[nativeKey]) }.toMap()
}

fun MediaSessionCompat.QueueItem.toMap(): Map<String, *> {
    return mapOf(
        "queueId" to queueId,
        "description" to description.toMap()
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

/**
 *  convert Bundle to Map object
 */
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


private fun RatingCompat.toMap(): Map<String, Any?> {
    return mapOf(
        "ratingStyle" to ratingStyle,
        "ratingValue" to percentRating
    )
}

private fun Map<*, *>?.toRating(): RatingCompat? {
    this ?: return null
    val value = get("ratingValue") as? Float ?: 0F
    return when (val style = get("ratingStyle") as? Int) {
        RatingCompat.RATING_HEART -> RatingCompat.newHeartRating(value > 0)
        RatingCompat.RATING_PERCENTAGE -> RatingCompat.newPercentageRating(value)
        RatingCompat.RATING_3_STARS, RatingCompat.RATING_4_STARS, RatingCompat.RATING_5_STARS ->
            RatingCompat.newStarRating(style, value)
        RatingCompat.RATING_THUMB_UP_DOWN -> RatingCompat.newThumbRating(value > 0)
        else -> null
    }

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