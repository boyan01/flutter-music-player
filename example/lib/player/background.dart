import 'package:music_player/music_player.dart';
import 'package:music_player_example/main.dart';

class ExamplePlayQueueInterceptor extends PlayQueueInterceptor {
  @override
  Future<MusicMetadata> onPlayNextNoMoreMusic(BackgroundPlayQueue queue, PlayMode playMode) async {
    if (queue.queueId == "fm") {
      return medias[2];
    }
    return await super.onPlayNextNoMoreMusic(queue, playMode);
  }

  @override
  Future<MusicMetadata> onPlayPreviousNoMoreMusic(BackgroundPlayQueue queue, PlayMode playMode) {
    if (queue.queueId == "fm") {
      return null;
    }
    return super.onPlayPreviousNoMoreMusic(queue, playMode);
  }
}
