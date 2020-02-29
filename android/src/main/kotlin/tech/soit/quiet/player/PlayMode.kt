package tech.soit.quiet.player

/**
 *
 * PlayMode of MusicPlayer
 *
 * @author 杨彬
 */
sealed class PlayMode(
    val rawValue: Int
) {
    companion object {
        fun valueOf(value: Int): PlayMode = when (value) {
            0 -> Shuffle
            1 -> Single
            2 -> Sequence
            else -> Undefined(value)
        }
    }

    //随机播放
    object Shuffle : PlayMode(0)

    //单曲循环
    object Single : PlayMode(1)

    //列表循环
    object Sequence : PlayMode(2)

    class Undefined(value: Int) : PlayMode(value)

}


