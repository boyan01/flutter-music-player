import 'package:music_player/src/player/play_queue.dart';

import 'music_metadata.dart';

abstract class PlaySequenceDelegate {
  /// [anchor] could be null
  Future<MusicMetadata>? getNext(
      PlayQueue queue, int playMode, MusicMetadata anchor);

  Future<MusicMetadata>? getPrevious(
      PlayQueue queue, int playMode, MusicMetadata anchor);
}

class DefaultPlaySequenceDelegate extends PlaySequenceDelegate {
  @override
  Future<MusicMetadata>? getNext(
      PlayQueue queue, int playMode, MusicMetadata anchor) {
    return null;
  }

  @override
  Future<MusicMetadata>? getPrevious(
      PlayQueue queue, int playMode, MusicMetadata anchor) {
    return null;
  }
}
