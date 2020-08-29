package tech.soit.quiet.service

import android.os.*
import tech.soit.quiet.MusicSessionCallback
import tech.soit.quiet.player.MusicMetadata
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.player.PlaybackState

internal class ShimMusicSessionCallback : MusicSessionCallback {

    private val callbacks = RemoteCallbackList<MusicSessionCallback>()

    private val handler = Handler(Looper.getMainLooper())

    private fun broadcast(action: MusicSessionCallback.() -> Unit) {
        // make sure broadcast in one thread.
        // does it need to switch to IO thread?
        handler.post {
            var i = callbacks.beginBroadcast()
            while (i > 0) {
                i--
                try {
                    callbacks.getBroadcastItem(i).action()
                } catch (e: RemoteException) {

                }
            }
            callbacks.finishBroadcast()
        }
    }

    fun addCallback(callback: MusicSessionCallback) {
        callbacks.register(callback)
    }

    fun removeCallback(callback: MusicSessionCallback) {
        callbacks.unregister(callback)
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        broadcast { onPlaybackStateChanged(state) }
    }

    override fun onPlayQueueChanged(queue: PlayQueue) {
        broadcast { onPlayQueueChanged(queue) }
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
        broadcast { onMetadataChanged(metadata) }
    }

    override fun onPlayModeChanged(playMode: Int) {
        broadcast { onPlayModeChanged(playMode) }
    }

    override fun asBinder(): IBinder {
        throw IllegalAccessError()
    }
}
