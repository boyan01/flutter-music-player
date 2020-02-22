package tech.soit.quiet.player

import android.content.Context
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tech.soit.quiet.MusicPlayerServicePlugin
import tech.soit.quiet.MusicPlayerSession
import tech.soit.quiet.MusicSessionCallback
import tech.soit.quiet.ext.mapPlaybackState
import tech.soit.quiet.ext.playbackError
import tech.soit.quiet.ext.toMediaSource
import tech.soit.quiet.service.ShimMusicSessionCallback

class MusicPlayerSessionImpl constructor(private val context: Context) : MusicPlayerSession.Stub(),
    CoroutineScope by MainScope() {

    companion object {
        private val audioAttribute = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    }

    // Wrap a SimpleExoPlayer with a decorator to handle audio focus for us.
    private val player: ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(context).apply {
            setAudioAttributes(audioAttribute, true)
            addListener(ExoPlayerEventListener())
        }
    }

    @Suppress("JoinDeclarationAndAssignment")
    internal val servicePlugin: MusicPlayerServicePlugin

    private val shimSessionCallback = ShimMusicSessionCallback()

    private var playMode: PlayMode = PlayMode.Sequence

    private var playQueue: PlayQueue = PlayQueue.Empty

    private var metadata: MusicMetadata? = null

    private fun performPlay(metadata: MusicMetadata?) {
        this.metadata = metadata
        if (metadata == null) {
            player.stop()
            return
        }
        player.prepare(metadata.toMediaSource(context, servicePlugin))
        player.playWhenReady = true
        invalidateMetadata()
    }


    override fun skipToNext() {
        skipTo { getNext(current) }
    }

    override fun skipToPrevious() {
        skipTo { getPrevious(current) }
    }

    override fun getPrevious(anchor: MusicMetadata?): MusicMetadata? {
        return playQueue.getPrevious(anchor, playMode)
    }

    private fun skipTo(call: suspend (PlayQueue) -> MusicMetadata?) {
        player.stop()
        val queue = playQueue
        launch {
            val next = runCatching { call(queue) }.getOrNull()
            performPlay(next)
        }
    }

    override fun play() {
        player.playWhenReady = true
    }


    override fun pause() {
        player.playWhenReady = false
    }

    override fun getPlayQueue(): PlayQueue {
        return playQueue
    }

    override fun getNext(anchor: MusicMetadata?): MusicMetadata? {
        return playQueue.getNext(current, playMode)
    }

    override fun setPlayQueue(queue: PlayQueue) {
        playQueue = queue
        invalidatePlayQueue()
    }

    override fun seekTo(pos: Long) {
        player.seekTo(pos)
    }

    override fun removeCallback(callback: MusicSessionCallback) {
        shimSessionCallback.removeCallback(callback)
    }

    override fun getPlaybackState(): PlaybackState {
        return playbackStateBackup
    }

    override fun stop() {
        player.stop()
    }

    override fun addCallback(callback: MusicSessionCallback) {
        shimSessionCallback.addCallback(callback)
    }

    override fun playFromMediaId(mediaId: String) {
        skipTo { it.getByMediaId(mediaId)/* TODO 向 service plugin 继续请求*/ }
    }

    override fun getPlayMode(): Int {
        return playMode.ordinal
    }

    override fun getCurrent(): MusicMetadata? {
        return metadata
    }

    override fun setPlayMode(playMode: Int) {
        this.playMode = PlayMode.values()[playMode]
        shimSessionCallback.onPlayModeChanged(playMode)
    }

    override fun addMetadata(metadata: MusicMetadata, anchorMediaId: String?) {
        playQueue.add(anchorMediaId, metadata)
        invalidatePlayQueue()
    }

    override fun removeMetadata(mediaId: String) {
        playQueue.remove(mediaId)
        invalidatePlayQueue()
    }

    private var playbackStateBackup: PlaybackState =
        PlaybackState(State.None, 0, 0, 1F, null, System.currentTimeMillis())

    private fun invalidatePlaybackState() {
        val playerError = player.playbackError()
        val state = playerError?.let { State.Error } ?: player.mapPlaybackState()
        val playbackState = PlaybackState(
            state = state,
            position = player.currentPosition,
            bufferedPosition = player.bufferedPosition,
            speed = player.playbackParameters.speed,
            error = playerError,
            updateTime = System.currentTimeMillis()
        )
        this.playbackStateBackup = playbackState
        shimSessionCallback.onPlaybackStateChanged(playbackState)
    }

    private fun invalidateMetadata() {
        val duration = if (player.duration == C.TIME_UNSET) 0 else player.duration
        shimSessionCallback.onMetadataChanged(metadata?.copyWith(duration = duration))
    }

    private fun invalidatePlayQueue() {
        shimSessionCallback.onPlayQueueChanged(playQueue)
    }

    override fun destroy() {
        cancel()
    }


    private inner class ExoPlayerEventListener : Player.EventListener {

        override fun onLoadingChanged(isLoading: Boolean) {
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            invalidatePlaybackState()
        }

        override fun onPositionDiscontinuity(reason: Int) {
            invalidatePlaybackState()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackStateInt: Int) {
            invalidatePlaybackState()
            // auto play next
            if (playbackStateInt == Player.STATE_ENDED) {
                skipTo { if (playMode == PlayMode.Single) current else getNext(current) }
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            invalidatePlaybackState()
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            invalidateMetadata()
        }

    }


    init {
        servicePlugin = MusicPlayerServicePlugin.startServiceIsolate(context, this)
    }

}
