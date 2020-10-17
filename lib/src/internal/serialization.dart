import 'package:music_player/music_player.dart';
import 'package:music_player/src/model/playback_error.dart';
import 'package:music_player/src/player/music_metadata.dart';
import 'package:music_player/src/player/play_queue.dart';
import 'package:music_player/src/player/playback_state.dart';

const String objectKeyPrefix = 'tech.soit.queit.player';

@deprecated
PlayQueue createPlayQueue(Map map) {
  return PlayQueue.fromMap(map);
}

BackgroundPlayQueue createBackgroundQueue(Map map) {
  return BackgroundPlayQueue(
    queueId: map['queueId'],
    queueTitle: map['queueTitle'],
    extras: map['extras'],
    queue: (map['queue'] as List).cast<Map>().map((e) => createMusicMetadata(e)).toList(),
    shuffleQueue: (map['shuffleQueue'] as List).cast<String>(),
  );
}

@deprecated
MusicMetadata createMusicMetadata(Map map) {
  return MusicMetadata.fromMap(map);
}

PlaybackState createPlaybackState(Map map) {
  return PlaybackState(
    state: PlayerState.values[map['state'] as int],
    position: map['position'] as int,
    bufferedPosition: map['bufferedPosition'] as int,
    speed: map['speed'] as double,
    error: _createPlaybackError(map['error'] as Map),
    updateTime: map['updateTime'] as int,
  );
}

PlaybackError _createPlaybackError(Map map) {
  if (map == null) {
    return null;
  }
  return PlaybackError(
    type: ErrorType.values[map["type"] as int],
    message: map["message"] ?? "Unknown Error.",
  );
}
