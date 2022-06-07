import 'package:music_player/music_player.dart';

extension MusicPlayerValueCopy on MusicPlayerValue {
  MusicPlayerValue copy({
    PlayMode? playMode,
    MusicMetadata? metadata,
    PlayQueue? queue,
    PlaybackState? state,
  }) {
    return MusicPlayerValue(
      playMode: playMode ?? this.playMode,
      metadata: metadata ?? this.metadata,
      queue: queue ?? this.queue,
      playbackState: state ?? this.playbackState,
    );
  }
}
