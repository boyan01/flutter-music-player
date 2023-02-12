import 'package:flutter/services.dart';

import '../../music_player.dart';

class PlayQueueInterceptor {
  Future<MusicMetadata?> onPlayNextNoMoreMusic(PlayMode playMode) =>
      Future.value(null);

  ///
  /// Throw MissingPluginException() to use default playNext behavior.
  /// Default Behavior:
  ///   1. playMode is [PlayMode.sequence], auto play queue first item
  ///   2. playMode is [PlayMode.shuffle], auto generate a new shuffle list, then play from first.
  ///   3. playMode is [PlayMode.undefined(any)]. stop play.
  ///
  /// return null to stop play.
  ///
  Future<List<MusicMetadata>> fetchMoreMusic(PlayMode playMode) {
    return Future.value(const <MusicMetadata>[]);
  }

  Future<MusicMetadata?> onPlayPreviousNoMoreMusic(PlayMode playMode) =>
      Future.value(null);
}

///
/// Interceptor when player try to load a media source
///
///
/// [mediaId] The id of media. [MediaMetadata.mediaId]
/// [fallbackUri] media origin uri.
///
/// @return media uri which should
///
typedef PlayUriInterceptor = Future<String> Function(
  String? mediaId,
  String? fallbackUri,
);

typedef ImageLoadInterceptor = Future<Uint8List> Function(
  MusicMetadata metadata,
);
