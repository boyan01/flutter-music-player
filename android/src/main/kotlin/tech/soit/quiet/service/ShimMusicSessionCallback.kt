package tech.soit.quiet.service

import android.os.IBinder
import tech.soit.quiet.MusicSessionCallback
import tech.soit.quiet.player.MusicMetadata
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.player.PlaybackState

internal class ShimMusicSessionCallback : MusicSessionCallback {

    private val callbacks = mutableListOf<MusicSessionCallback>()


    fun addCallback(callback: MusicSessionCallback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: MusicSessionCallback) {
        callbacks.remove(callback)
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        callbacks.forEach { it.onPlaybackStateChanged(state) }
    }

    override fun onPlayQueueChanged(queue: PlayQueue) {
        callbacks.forEach { it.onPlayQueueChanged(queue) }
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
        callbacks.forEach { it.onMetadataChanged(metadata) }
    }

    override fun onPlayModeChanged(playMode: Int) {
        callbacks.forEach { it.onPlayModeChanged(playMode) }
    }

    override fun asBinder(): IBinder {
        throw IllegalAccessError()
    }
}