import 'package:music_player/music_player.dart';

const String objectKeyPrefix = 'tech.soit.queit.player';

PlaybackState createPlaybackState(Map map) {
  return PlaybackState(
    state: PlayerState.values[map['state'] as int],
    position: map['position'] as int,
    bufferedPosition: map['bufferedPosition'] as int,
    speed: map['speed'] as double,
    error: _createPlaybackError(map['error'] as Map?),
    updateTime: map['updateTime'] as int,
  );
}

PlaybackError? _createPlaybackError(Map? map) {
  if (map == null) {
    return null;
  }
  return PlaybackError(
    type: ErrorType.values[map["type"] as int],
    message: map["message"] ?? "Unknown Error.",
  );
}
