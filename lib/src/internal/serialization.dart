import 'package:music_player/music_player.dart';
import 'package:music_player/src/player/music_metadata.dart';
import 'package:music_player/src/player/play_queue.dart';
import 'package:music_player/src/player/playback_state.dart';

const String objectKeyPrefix = 'tech.soit.queit.player';

extension PlayQueueJson on PlayQueue {
  Map toMap() {
    return {
      "queueId": queueId,
      "queueTitle": queueTitle,
      "extras": extras,
      "queue": queue.map((e) => e.toMap()).toList(),
    };
  }
}

extension MusicMetadataJson on MusicMetadata {
  Map toMap() {
    return {
      "extras": extras,
      "mediaId": mediaId,
      "title": title,
      "subtitle": subtitle,
      "duration": duration,
      "iconUri": iconUri,
      "mediaUri": mediaUri,
    };
  }
}

PlayQueue createPlayQueue(Map map) {
  return PlayQueue(
    queueId: map['queueId'],
    queueTitle: map['queueTitle'],
    extras: map['extras'],
    queue: (map['queue'] as List).cast<Map>().map((e) => createMusicMetadata(e)).toList(),
  );
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

MusicMetadata createMusicMetadata(Map map) {
  return MusicMetadata(
    extras: map["extras"],
    mediaId: map["mediaId"],
    title: map["title"],
    subtitle: map["subtitle"],
    duration: map["duration"],
    iconUri: map["iconUri"],
    mediaUri: map['mediaUri'],
  );
}

PlaybackState createPlaybackState(Map map) {
  DateTime.now();
  return PlaybackState(
    state: PlayerState.values[map['state'] as int],
    position: map['position'] as int,
    bufferedPosition: map['bufferedPosition'] as int,
    speed: map['speed'] as double,
    error: map['error'],
    updateTime: map['updateTime'] as int,
  );
}
