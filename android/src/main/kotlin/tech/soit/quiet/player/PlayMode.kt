package tech.soit.quiet.player

import android.annotation.SuppressLint
import android.support.v4.media.session.PlaybackStateCompat

/**
 *
 * PlayMode of MusicPlayer
 *
 * @author 杨彬
 */
enum class PlayMode {
    //随机播放
    Shuffle,
    //单曲循环
    Single,
    //列表循环
    Sequence;

    companion object {

        /**
         * safely convert enum name to instance
         */
        @SuppressLint("DefaultLocale")
        fun from(name: String?) = when (name?.toLowerCase()) {
            "shuffle" -> Shuffle
            "single" -> Single
            else -> Sequence
        }

    }

    fun shuffleMode() = when (this) {
        Shuffle -> PlaybackStateCompat.SHUFFLE_MODE_ALL
        Single -> PlaybackStateCompat.SHUFFLE_MODE_NONE
        Sequence -> PlaybackStateCompat.SHUFFLE_MODE_NONE
    }

    fun repeatMode() = when (this) {
        Shuffle -> PlaybackStateCompat.REPEAT_MODE_ALL
        Single -> PlaybackStateCompat.REPEAT_MODE_ONE
        Sequence -> PlaybackStateCompat.REPEAT_MODE_ALL
    }


}


interface PlayModeContainer {

    var playMode: PlayMode

}