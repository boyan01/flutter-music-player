import 'package:flutter/foundation.dart';
import 'package:music_player/music_player.dart';

abstract class Player extends ChangeNotifier implements ValueListenable<MusicPlayerValue> {
  /// Current player state value.
  MusicPlayerValue get value => MusicPlayerValue(
        queue: queue.value,
        playMode: playMode.value,
        playbackState: playbackState.value,
        metadata: metadata.value,
      );

  /// Current playing music metadata.
  ValueListenable<MusicMetadata> get metadata;

  /// Current playing music queue.
  ValueListenable<PlayQueue> get queue;

  /// Current player's playback state.
  ValueListenable<PlaybackState> get playbackState;

  /// Current play mode.
  /// determine how to play [queue].
  ValueListenable<PlayMode> get playMode;
}
