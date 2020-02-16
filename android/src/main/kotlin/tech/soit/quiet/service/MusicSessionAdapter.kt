package tech.soit.quiet.service

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import tech.soit.quiet.MusicSessionCallback
import tech.soit.quiet.player.*
import tech.soit.quiet.utils.ArtworkCache
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


private const val supportActions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
        PlaybackStateCompat.ACTION_PLAY or
        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
        PlaybackStateCompat.ACTION_PAUSE or
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

    override fun onPlayQueueChanged(queue: PlayQueue) {
        mediaSession.setQueueTitle(queue.queueTitle)
        mediaSession.setQueue(queue.getQueue()
            .map { it.toMediaMetadata() }
            .mapIndexed { index, metadata ->
                MediaSessionCompat.QueueItem(metadata.description, index.toLong())
            })
    }

    override fun onPlayModeChanged(playMode: Int) {
        mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
    }

}

private fun MusicMetadata.toMediaMetadata(): MediaMetadataCompat {
    return MediaMetadataCompat.Builder()
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration ?: 0L)
        .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUri)
        .putBitmap(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
            ArtworkCache.get(ArtworkCache.key(this))?.bitmap
        )
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