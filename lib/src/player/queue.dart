import 'package:music_player/src/player/play_queue.dart';

import 'music_metadata.dart';

abstract class PlaySequenceDelegate {
  /// [anchor] could be null
  Future<MusicMetadata> getNext(PlayQueue queue, int playMode, MusicMetadata anchor);

  Future<MusicMetadata> getPrevious(PlayQueue queue, int playMode, MusicMetadata anchor);
}

class DefaultPlaySequenceDelegate extends PlaySequenceDelegate {
  static const int MODE_SEQUENCE = 0;

  static const int MODE_SINGLE = 1;

  static const int MODE_SHUFFLE = 2;

  bool _checkPlayMode(int playMode) {
    return playMode == MODE_SEQUENCE || playMode == MODE_SINGLE || playMode == MODE_SHUFFLE;
  }

  @override
  Future<MusicMetadata> getNext(PlayQueue queue, int playMode, MusicMetadata anchor) {
    assert(_checkPlayMode(playMode));
    return null;
  }

  @override
  Future<MusicMetadata> getPrevious(PlayQueue queue, int playMode, MusicMetadata anchor) {
    assert(_checkPlayMode(playMode));
    return null;
  }
}
