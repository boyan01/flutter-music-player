import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:music_player/music_player.dart';
import 'package:music_player/src/internal/serialization.dart';

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
