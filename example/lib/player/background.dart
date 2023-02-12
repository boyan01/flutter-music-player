import 'package:flutter/services.dart';
import 'package:music_player/music_player.dart';
import 'package:music_player_example/main.dart';

class ExamplePlayQueueInterceptor extends PlayQueueInterceptor {
  @override
  Future<List<MusicMetadata>> fetchMoreMusic(PlayMode playMode) async {
    if (playMode.index == 4) {
      return medias.getRange(1, 3).toList();
    }
    throw MissingPluginException();
  }
}
