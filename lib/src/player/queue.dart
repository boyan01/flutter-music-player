import 'package:music_player/music_player.dart';

class PlayQueue {
  final String queueId;
  final String queueTitle;
  final List<MediaMetadata> items;

  PlayQueue(this.queueId, this.items, this.queueTitle);

  Future<MediaMetadata> getNext(MediaMetadata metadata) {}

  Future<MediaMetadata> getPrevious(MediaMetadata metadata) {}
}

abstract class PlaySequenceDelegate {
  /// [anchor] could be null
  Future<MediaMetadata> getNext(PlayQueue queue, int playMode, MediaMetadata anchor);

  Future<MediaMetadata> getPrevious(PlayQueue queue, int playMode, MediaMetadata anchor);
}

class DefaultPlaySequenceDelegate extends PlaySequenceDelegate {
  @override
  Future<MediaMetadata> getNext(PlayQueue queue, int playMode, MediaMetadata anchor) {
    return null;
  }

  @override
  Future<MediaMetadata> getPrevious(PlayQueue queue, int playMode, MediaMetadata anchor) {
    return null;
  }
}
