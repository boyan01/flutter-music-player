import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

// PlayerPlugin channel
const MethodChannel _channel = const MethodChannel('tech.soit.quiet/player');

class MusicPlayer {
  void _listenNative() {
    _channel.setMethodCallHandler((call) async {
      debugPrint("call from native: ${call.method} , arg = ${call.arguments}");
    });
  }

  void setPlayList() {}
}

