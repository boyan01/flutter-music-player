import 'package:flutter/services.dart';
import 'package:music_player/music_player.dart';
import 'play_mode.dart';

///
/// Interface for controlling media playback.
///
/// [play]
/// [pause]
/// [playFromMediaId]
/// [skipToNext]
/// [skipToPrevious]
/// [seekTo]
/// [setPlayMode]
///
///
class TransportControls {
  final MethodChannel _channel;

  TransportControls(this._channel);

  Future<void> play() async {
    await _channel.invokeMethod("play");
  }

  Future<void> playFromMediaId(String mediaId) async {
    await _channel.invokeMethod("playFromMediaId", mediaId);
  }

  Future<void> prepareFromMediaId(String mediaId) async {
    await _channel.invokeMethod("prepareFromMediaId", mediaId);
  }

  Future<void> pause() async {
    await _channel.invokeMethod("pause");
  }

  Future<void> seekTo(int pos) async {
    await _channel.invokeMethod("seekTo", pos);
  }

  Future<void> skipToNext() async {
    await _channel.invokeMethod("skipToNext");
  }

  Future<void> skipToPrevious() async {
    await _channel.invokeMethod("skipToPrevious");
  }

  Future<void> setPlayMode(PlayMode playMode) async {
    await _channel.invokeMethod("setPlayMode", playMode.index);
  }

  Future<void> setPlaybackSpeed(double rate) async {
    await _channel.invokeMethod("setPlaybackSpeed", rate);
  }
}
