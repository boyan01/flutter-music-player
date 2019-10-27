import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:music_player/music_player.dart';
import 'package:music_player/src/model/media_metadata.dart';
import 'package:music_player/src/model/playback_state.dart';
import 'package:music_player/src/model/rating.dart';
import 'package:music_player/src/player/player_channel.dart';

import 'play_mode.dart';

///
/// Interface for controlling media playback.
///
class TransportControls {
  final MethodChannel _channel;

  TransportControls(this._channel);

  Future<void> prepareFromMediaId(String mediaId) async {
    await _channel.invokeMethod("prepareFromMediaId", mediaId);
  }

  Future<void> play() async {
    await _channel.invokeMethod("play");
  }

  Future<void> playFromMediaId(String mediaId) async {
    await _channel.invokeMethod("playFromMediaId", mediaId);
  }

  Future<void> pause() async {
    await _channel.invokeMethod("pause");
  }

  Future<void> stop() async {
    await _channel.invokeMethod("stop");
  }

  Future<void> seekTo(int pos) async {
    await _channel.invokeMethod("seekTo", pos);
  }

  Future<void> fastForward() async {
    await _channel.invokeMethod("fastForward");
  }

  Future<void> skipToNext() async {
    await _channel.invokeMethod("skipToNext");
  }

  Future<void> rewind() async {
    await _channel.invokeMethod("rewind");
  }

  Future<void> skipToPrevious() async {
    await _channel.invokeMethod("skipToPrevious");
  }

  Future<void> setRating(Rating rating) async {
    await _channel.invokeMethod("setRating", rating.toMap());
  }

  Future<void> setPlayMode(PlayMode playMode) async {
    await _channel.invokeMethod("setPlayMode", playModeToStr(playMode));
  }
}

mixin MediaControllerCallback on ValueNotifier<MusicPlayerValue> {
  @protected
  bool handleMediaControllerCallbackMethod(MethodCall call) {
    switch (call.method) {
      case "onSessionReady":
        onSessionReady();
        break;
      case "onSessionDestroyed":
        onSessionDestroyed();
        break;
      case "onPlaybackStateChanged":
        final value = PlaybackState.fromMap((call.arguments as Map)?.cast());
        onPlaybackStateChanged(value);
        break;
      case "onMetadataChanged":
        onMetadataChanged(MediaMetadata.fromMap((call.arguments as Map)?.cast()));
        break;
      case "onAudioInfoChanged":
        onAudioInfoChanged();
        break;
      case "onPlayListChanged":
        onPlayListChanged(PlayList.fromMap(call.arguments));
        break;
      default:
        return false;
    }
    return true;
  }

  void onSessionReady() {
    notifyListeners();
  }

  void onSessionDestroyed() {
    value = const MusicPlayerValue.none();
    notifyListeners();
  }

  void onPlaybackStateChanged(PlaybackState playbackState) {
    value = value.copyWith(playbackState: playbackState);
    notifyListeners();
  }

  void onMetadataChanged(MediaMetadata metadata) {
    value = value.setMetadata(metadata);
    notifyListeners();
  }

  void onAudioInfoChanged() {
    //TODO audio info
    value = value.copyWith(playbackInfo: null);
    notifyListeners();
  }

  void onPlayListChanged(PlayList playList) {
    value = value.copyWith(playList: playList);
    notifyListeners();
  }
}
