import 'music_metadata.dart';
import 'play_queue.dart';
import 'playback_state.dart';
import 'play_mode.dart';

abstract class MusicPlayerCallback {
  void onPlaybackStateChanged(PlaybackState state);

  void onMetadataChange(MusicMetadata metadata);

  void onPlayQueueChanged(PlayQueue queue);

  void onPlayModeChanged(PlayMode playMode);
}
