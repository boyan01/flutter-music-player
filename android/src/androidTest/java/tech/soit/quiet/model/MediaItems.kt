package tech.soit.quiet.model

import android.support.v4.media.MediaMetadataCompat


val bamboo: MediaMetadataCompat =
    MediaMetadataCompat.Builder()
        .apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "竹林间")
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "忘川风华录")
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "三无")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "bamboo")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "asset:///bamboo.mp3")
        }
        .build()


val rise: MediaMetadataCompat =
    MediaMetadataCompat.Builder()
        .apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "RISE")
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "登峰造极境—英雄联盟2018全球总决赛主题曲")
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "RISE")
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "The Glitch Mob")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "rise")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "asset:///rise.mp3")
        }
        .build()


val hide: MediaMetadataCompat =
    MediaMetadataCompat.Builder()
        .apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "藏")
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "徐梦圆同名专辑")
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "徐梦圆")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "hide")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "asset:///hide.mp3")
        }
        .build()
