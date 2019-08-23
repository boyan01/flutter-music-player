import 'dart:async';

import 'package:flutter/services.dart';

class FlutterMusicPlayer {
  static const MethodChannel _channel = const MethodChannel('flutter_music_player');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
