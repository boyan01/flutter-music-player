package tech.soit.quiet.service

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import tech.soit.quiet.MusicSessionCallback
import tech.soit.quiet.player.*
import tech.soit.quiet.utils.LoggerLevel
import tech.soit.quiet.utils.log


class MediaSessionCallbackAdapter(
        private val playerSession: MusicPlayerSessionImpl
) : MediaSessionCompat.Callback() {


    override fun onPlay() {
        playerSession.play()
    }

    override fun onPause() {
        playerSession.pause()
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        if (mediaId.isNullOrEmpty()) {
            log(LoggerLevel.ERROR) { "can not play empty mediaId" }
            return
        }
        playerSession.playFromMediaId(mediaId)
    }

    override fun onSeekTo(pos: Long) {
        playerSession.seekTo(pos)
    }

    override fun onSkipToNext() {
        playerSession.skipToNext()
    }

    override fun onSkipToPrevious() {
        playerSession.skipToPrevious()
    }

    override fun onStop() {
        playerSession.stop()
    }

}


private const val supportActions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT and
        PlaybackStateCompat.ACTION_PLAY and
        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID and
        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS and
        PlaybackStateCompat.ACTION_PAUSE and
        PlaybackStateCompat.ACTION_STOP

class MusicSessionCallbackAdapter(
        private val mediaSession: MediaSessionCompat
) : MusicSessionCallback.Stub() {

    override fun onPlaybackStateChanged(state: PlaybackState) {
        val builder = PlaybackStateCompat.Builder()

        builder.setActions(supportActions)
                .setState(state.stateCode, state.position, state.speed)
                .setBufferedPosition(state.bufferedPosition)
                .setActiveQueueItemId(0) //TODO queue item id
        if (state.error != null) {
            builder.setErrorMessage(state.error.errorCode, state.error.errorMessage)
        }
        mediaSession.setPlaybackState(builder.build())
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
        mediaSession.setMetadata(metadata?.toMediaMetadata())
    }

    override fun onPlayQueueChanged(queue: PlayQueue?) {

    }

    override fun onPlayModeChanged(playMode: Int) {
        mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
    }

}

private fun MusicMetadata.toMediaMetadata(): MediaMetadataCompat {
    return MediaMetadataCompat.Builder()
            .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subTitle)
            .build()
}

private val PlaybackState.stateCode: Int
    get() {
        return when (state) {
            State.None -> PlaybackStateCompat.STATE_NONE
            State.Paused -> PlaybackStateCompat.STATE_PAUSED
            State.Playing -> PlaybackStateCompat.STATE_PLAYING
            State.Buffering -> PlaybackStateCompat.STATE_BUFFERING
            State.Error -> PlaybackStateCompat.STATE_ERROR
        }
    }