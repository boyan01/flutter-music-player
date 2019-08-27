import 'package:flutter/services.dart';
import 'package:music_player/src/model/media_metadata.dart';
import 'package:music_player/src/model/playback_state.dart';
import 'package:music_player/src/model/rating.dart';

///
/// Interface for controlling media playback.
///
class TransportControls {
  final MethodChannel _channel;

  TransportControls(this._channel);

  void prepare() {
    _channel.invokeMethod("prepare");
  }

  void prepareFromMediaId(String mediaId, Map extras) {
    _channel.invokeMethod("prepareFromMediaId", extras);
  }

  void play() {
    _channel.invokeMethod("play");
  }

  void playFromMediaId(String mediaId, Map extras) {
    _channel.invokeMethod("playFromMediaId", extras);
  }

  void skipToQueueItem(int id) {
    _channel.invokeMethod("skipToQueueItem", id);
  }

  void pause() {
    _channel.invokeMethod("pause");
  }

  void stop() {
    _channel.invokeMethod("stop");
  }

  void seekTo(int pos) {
    _channel.invokeMethod("seekTo", pos);
  }

  void fastForward() {
    _channel.invokeMethod("fastForward");
  }

  void skipToNext() {
    _channel.invokeMethod("skipToNext");
  }

  void rewind() {
    _channel.invokeMethod("rewind");
  }

  void skipToPrevious() {
    _channel.invokeMethod("skipToPrevious");
  }

  void setRating(Rating rating) {
    _channel.invokeMethod("setRating", rating.toMap());
  }

  void setRepeatMode(int repeatMode) {
    _channel.invokeMethod("setRepeatMode", repeatMode);
  }

  void setShuffleMode(int shuffleMode) {
    _channel.invokeMethod("setShuffleMode", shuffleMode);
  }
}

mixin MediaControllerCallback {
  bool handleMediaControllerCallbackMethod(MethodCall call) {
    switch (call.method) {
      case "onSessionReady":
        onSessionReady();
        break;
      case "onSessionDestroyed":
        onSessionDestroyed();
        break;
      case "onPlaybackStateChanged":
        onPlaybackStateChanged(PlaybackState.fromMap(call.arguments));
        break;
      case "onMetadataChanged":
        onMetadataChanged(MediaMetadata.fromMap(call.arguments));
        break;
      case "onAudioInfoChanged":
        onAudioInfoChanged();
        break;
      case "onRepeatModeChanged":
        onRepeatModeChanged(call.arguments);
        break;
      case "onShuffleModeChanged":
        onShuffleModeChanged(call.arguments);
        break;
      default:
        return false;
    }
    return true;
  }

  void onSessionReady() {}

  void onSessionDestroyed() {}

  void onPlaybackStateChanged(PlaybackState playbackState) {}

  void onMetadataChanged(MediaMetadata metadata) {}

  void onAudioInfoChanged() {}

  void onRepeatModeChanged(int repeatMode) {}

  void onShuffleModeChanged(int shuffleMode) {}
}
