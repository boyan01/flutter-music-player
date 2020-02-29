import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:music_player/music_player.dart';
import 'package:music_player/src/internal/serialization.dart';

import 'internal/player_callback_adapter.dart';

///
/// Interceptor when player try to load a media source
///
///
/// [mediaId] The id of media. [MediaMetadata.mediaId]
/// [fallbackUri] media origin uri.
///
/// @return media uri which should
///
typedef PlayUriInterceptor = Future<String> Function(String mediaId, String fallbackUri);

typedef ImageLoadInterceptor = Future<Uint8List> Function(MusicMetadata metadata);

class Config {
  final bool enableCache;

  final String userAgent;

  const Config({this.enableCache = false, this.userAgent});

  Map<String, dynamic> toMap() {
    return {
      'enableCache': this.enableCache,
      'userAgent': this.userAgent,
    };
  }
}

///
/// handle background callback
///
Future runBackgroundService({
  Config config = const Config(),
  PlayUriInterceptor playUriInterceptor,
  ImageLoadInterceptor imageLoadInterceptor,
}) async {
  WidgetsFlutterBinding.ensureInitialized();
  // decrease background image memory
  PaintingBinding.instance.imageCache.maximumSize = 20 << 20; // 20 MB
  final backgroundChannel = MethodChannel("tech.soit.quiet/background_callback");
  final backgroundPlayer = BackgroundMusicPlayer._internal();
  backgroundChannel.setMethodCallHandler((call) async {
    switch (call.method) {
      case 'loadImage':
        if (imageLoadInterceptor != null) {
          return await imageLoadInterceptor(createMusicMetadata(call.arguments));
        }
        throw MissingPluginException();
      case 'getPlayUrl':
        if (playUriInterceptor != null) {
          final String id = call.arguments['id'];
          final String fallbackUrl = call.arguments['url'];
          return await playUriInterceptor(id, fallbackUrl);
        }
        throw MissingPluginException();
      default:
        throw MissingPluginException("can not hanle : ${call.method} ");
    }
  });
  backgroundChannel.invokeMethod('updateConfig', config.toMap());
}

class BackgroundMusicPlayer extends ValueNotifier<MusicPlayerValue> with ChannelPlayerCallbackAdapter {
  final _uiChannel = MethodChannel("tech.soit.quiet/player.ui");

  BackgroundMusicPlayer._internal() : super(MusicPlayerValue.none()) {
    _uiChannel.setMethodCallHandler((call) async {
      if (handleRemoteCall(call)) {
        return;
      }
      throw new UnimplementedError();
    });
    _uiChannel.invokeMethod("init");
  }
}

abstract class BackgroundPlayerCallback {
  void onPlaybackStateChanged(BackgroundMusicPlayer player, PlaybackState state);

  void onMetadataChange(BackgroundMusicPlayer player, MusicMetadata metadata);

  void onPlayQueueChanged(BackgroundMusicPlayer player, PlayQueue queue);

  void onPlayModeChanged(BackgroundMusicPlayer player, PlayMode playMode);
}
