package tech.soit.quiet.ext

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import tech.soit.quiet.player.PlayerError
import tech.soit.quiet.player.State

fun ExoPlayer.playbackError(): PlayerError? {
    if (playbackState != Player.STATE_IDLE) return null
    val error = playbackError ?: return null
    val errorType = when (error.type) {
        ExoPlaybackException.TYPE_SOURCE -> PlayerError.TYPE_SOURCE
        ExoPlaybackException.TYPE_RENDERER -> PlayerError.TYPE_RENDERER
        else -> PlayerError.TYPE_UNKNOWN
    }
    return PlayerError(errorType, error.message ?: " $error")
}


fun ExoPlayer.mapPlaybackState(): State {
    return when (playbackState) {
        Player.STATE_BUFFERING -> State.Buffering
        Player.STATE_READY -> if (playWhenReady) State.Playing else State.Paused
        Player.STATE_ENDED -> State.Paused
        Player.STATE_IDLE -> State.None
        else -> State.Paused
    }
}

fun Player.durationOrZero(): Long {
    val duration = duration
    return if (duration == C.TIME_UNSET) 0 else duration
}
