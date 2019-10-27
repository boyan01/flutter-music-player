package tech.soit.quiet.utils

import android.support.v4.media.MediaMetadataCompat

val MediaMetadataCompat.mediaId: String
    get() = requireNotNull(getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
            { " can not obtain media id from $this" })