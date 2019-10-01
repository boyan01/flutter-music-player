import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:music_player/music_player.dart';

///
/// Interceptor when player try to load a media source
///
///
/// [mediaId] The id of media. [MediaMetadata.mediaId]
/// [fallbackUri] media origin uri.
///
/// @return media uri which should
///
typedef PlayUriInterceptor = String Function(String mediaId, String fallbackUri);


typedef ImageLoadInterceptor = List<int> Function(MediaDescription description);


class Config {

  final bool enableCache;

  final String userAgent;

  const Config({this.enableCache = false, this.userAgent = null});

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
  PlayUriInterceptor playUriInterceptor = null,
  ImageLoadInterceptor imageLoadInterceptor = null,
}) async {
  WidgetsFlutterBinding.ensureInitialized();
  final backgroundChannel = MethodChannel("tech.soit.quiet/background_callback");
  backgroundChannel.setMethodCallHandler((call) async {
    switch (call.method) {
      case 'loadImage':
        if (imageLoadInterceptor != null) {
          final MediaDescription descirption = MediaDescription.fromMap(call.arguments);
          return imageLoadInterceptor(descirption);
        }
        throw MissingPluginException();
      case 'getPlayUrl':
        if (playUriInterceptor != null) {
          final String id = call.arguments['id'];
          final String fallbackUrl = call.arguments['url'];
          return playUriInterceptor(id, fallbackUrl);
        }
        throw MissingPluginException();
      default:
        throw MissingPluginException("can not hanle : ${call.method} ");
    }
  });
  backgroundChannel.invokeMethod('updateConfig', config.toMap());
  debugPrint("background : runBackgroundService over");
}
