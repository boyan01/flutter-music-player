package tech.soit.quiet

import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.MethodChannel
import tech.soit.quiet.player.MusicMetadata
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.player.PlaybackState

class MusicPlayerCallbackPlugin constructor(
    private val methodChannel: MethodChannel
) : MusicSessionCallback.Stub() {

    private val handler = Handler(Looper.getMainLooper())

    private fun ui(action: () -> Unit) {
        handler.post(action)
    }

    override fun onPlaybackStateChanged(state: PlaybackState) = ui {
        methodChannel.invokeMethod("onPlaybackStateChanged", state.toMap())
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) = ui {
        methodChannel.invokeMethod("onMetadataChanged", metadata?.obj)
    }

    override fun onPlayQueueChanged(queue: PlayQueue) = ui {
        methodChannel.invokeMethod("onPlayQueueChanged", queue.toDartMapObject())
    }

    override fun onPlayModeChanged(playMode: Int) = ui {
        methodChannel.invokeMethod("onPlayModeChanged", playMode)
    }

}
