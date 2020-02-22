// MusicSessionCallback.aidl
package tech.soit.quiet;

import tech.soit.quiet.player.PlaybackState;
import tech.soit.quiet.player.MusicMetadata;
import tech.soit.quiet.player.PlayQueue;

interface MusicSessionCallback {

    void onPlaybackStateChanged(in PlaybackState state);

    void onMetadataChanged(in MusicMetadata metadata);

    void onPlayQueueChanged(in PlayQueue queue);

    void onPlayModeChanged(int playMode);

}
