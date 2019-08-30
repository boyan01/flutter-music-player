import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:music_player/music_player.dart';
import 'package:music_player/src/model/media_metadata.dart';
import 'package:music_player/src/model/playback_state.dart';
import 'package:music_player/src/model/rating.dart';
import 'package:music_player/src/player/player_channel.dart';

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
    _channel.invokeMethod("prepareFromMediaId", {
      "mediaId": mediaId,
      "extras": extras,
    });
  }

  void play() {
    _channel.invokeMethod("play");
  }

  void playFromMediaId(String mediaId, List<MediaMetadata> queue, String queueTitle) {
    _channel.invokeMethod("playFromMediaId", {
      "mediaId": mediaId,
      "queue": queue.map((it) => it.toMap()).toList(),
      "queueTitle": queueTitle,
    });
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

mixin MediaControllerCallback on ValueNotifier<MusicPlayerState> {
  bool handleMediaControllerCallbackMethod(MethodCall call) {
    switch (call.method) {
      case "onInit":
        value = MusicPlayerState.fromMap(call.arguments) ?? MusicPlayerState.none();
        break;
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
      case "onRepeatModeChanged":
        onRepeatModeChanged(call.arguments);
        break;
      case "onShuffleModeChanged":
        onShuffleModeChanged(call.arguments);
        break;
      case 'onQueueChanged':
        final List list = call.arguments as List ?? const [];
        onQueueChanged(list.cast<Map>().map((it) => QueueItem.fromMap(it.cast())).toList());
        break;
      case 'onQueueTitleChanged':
        onQueueTitleChanged(call.arguments);
        break;
      default:
        return false;
    }
    return true;
  }

  void onSessionReady() {
    value = value.copyWith();
    notifyListeners();
  }

  void onSessionDestroyed() {
    value = const MusicPlayerState.none();
    notifyListeners();
  }

  void onPlaybackStateChanged(PlaybackState playbackState) {
    value = value.copyWith(playbackState: playbackState);
    debugPrint("onPlaybackStateChanged: $playbackState");
    notifyListeners();
  }

  void onMetadataChanged(MediaMetadata metadata) {
    //FIXME metadata be null
    value = value.copyWith(metadata: metadata);
    notifyListeners();
  }

  void onAudioInfoChanged() {
    //TODO audio info
    value = value.copyWith(playbackInfo: null);
    notifyListeners();
  }

  void onRepeatModeChanged(int repeatMode) {
    value = value.copyWith(repeatMode: repeatMode);
    notifyListeners();
  }

  void onQueueChanged(List<QueueItem> queue) {
    value = value.copyWith(queue: queue);
    notifyListeners();
  }

  void onQueueTitleChanged(String title) {
    value = value.copyWith(queueTitle: title);
    notifyListeners();
  }

  void onShuffleModeChanged(int shuffleMode) {
    value = value.copyWith(shuffleMode: shuffleMode);
    notifyListeners();
  }
}
