package tech.soit.quiet

import io.flutter.plugin.common.MethodChannel
import tech.soit.quiet.player.MusicMetadata
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.player.PlaybackState

class MusicPlayerCallbackPlugin constructor(
    private val methodChannel: MethodChannel
) : MusicSessionCallback.Stub() {

    override fun onPlaybackStateChanged(state: PlaybackState) {
        methodChannel.invokeMethod("onPlaybackStateChanged", state.toMap())
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
        methodChannel.invokeMethod("onMetadataChanged", metadata?.obj)
    }

    override fun onPlayQueueChanged(queue: PlayQueue) {
        methodChannel.invokeMethod("onPlayQueueChanged", queue.toDartMapObject())
    }

    override fun onPlayModeChanged(playMode: Int) {
        methodChannel.invokeMethod("onPlayModeChanged", playMode)
    }

}