import 'package:flutter/foundation.dart';
import 'package:music_player/music_player.dart';

abstract class Player extends ChangeNotifier implements ValueListenable<MusicPlayerValue> {
  /// Current player state value.
  MusicPlayerValue get value => MusicPlayerValue(
        queue: queue,
        playMode: playMode,
        playbackState: playbackState,
        metadata: metadata,
      );

  /// See [metadataListenable].
  MusicMetadata get metadata => metadataListenable.value;

  /// See [queueListenable].
  PlayQueue get queue => queueListenable.value;

  /// See [playbackStateListenable].
  PlaybackState get playbackState => playbackStateListenable.value;

  /// See [playModeListenable]
  PlayMode get playMode => playModeListenable.value;

  /// Current playing music metadata.
  ValueListenable<MusicMetadata> get metadataListenable;

  /// Current playing music queue.
  ValueListenable<PlayQueue> get queueListenable;

  /// Current player's playback state.
  ValueListenable<PlaybackState> get playbackStateListenable;

  /// Current play mode.
  /// determine how to play [queueListenable].
  ValueListenable<PlayMode> get playModeListenable;
}
