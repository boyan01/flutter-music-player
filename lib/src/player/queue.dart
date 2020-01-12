import 'package:music_player/music_player.dart';

class PlayQueue {
  final String queueId;
  final String queueTitle;
  final List<MediaMetadata> items;

  PlayQueue(this.queueId, this.items, this.queueTitle);

  Future<MediaMetadata> getNext(MediaMetadata metadata) {

  }

  Future<MediaMetadata> getPrevious(MediaMetadata metadata) {

  }
}
