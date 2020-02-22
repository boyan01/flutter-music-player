package tech.soit.quiet.utils

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat


fun MediaDescriptionCompat.builder(): MediaDescriptionCompat.Builder {
    return MediaDescriptionCompat.Builder()
        .setDescription(description)
        .setExtras(extras)
        .setTitle(title)
        .setSubtitle(subtitle)
        .setIconBitmap(iconBitmap)
        .setIconUri(iconUri)
        .setMediaId(mediaId)
        .setMediaUri(mediaUri)
}

