package tech.soit.quiet.player

import tech.soit.quiet.MusicSessionCallback

abstract class BaseMusicSessionCallback : MusicSessionCallback.Stub() {
    override fun onPlaybackStateChanged(state: PlaybackState) {
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
    }

    override fun onPlayQueueChanged(queue: PlayQueue) {
    }

    override fun onPlayModeChanged(playMode: Int) {
    }
}