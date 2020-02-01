import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:music_player/src/internal/player_callback_adapter.dart';
import 'package:music_player/src/player/queue.dart';

const _uiChannel = MethodChannel("tech.soit.quiet/player.ui");

/// MusicPlayer for UI interaction.
class MusicPlayer extends ValueNotifier<MusicPlayerValue> with ChannelPlayerCallbackAdapter {
  static MusicPlayer _player;

  MusicPlayer._internal() : super(null) {
    _uiChannel.setMethodCallHandler((call) async {
      if (handleRemoteCall(call)) {
        return;
      }
      throw new UnimplementedError();
    });
  }

  factory MusicPlayer() {
    if (_player == null) {
      _player = MusicPlayer._internal();
    }
    return _player;
  }

  void setPlayQueue(PlayQueue queue) {}
}

class MusicPlayerValue {}
