import 'package:music_player/src/channel_ui.dart';
import 'package:music_player/src/player/music_metadata.dart';
import 'package:music_player/src/player/play_queue.dart';
import 'package:music_player/src/player/playback_state.dart';

extension MusicPlayerValueCopy on MusicPlayerValue {
  MusicPlayerValue copy({
    int playMode,
    MusicMetadata metadata,
    PlayQueue queue,
    PlaybackState state,
  }) {
    return MusicPlayerValue();
  }
}
