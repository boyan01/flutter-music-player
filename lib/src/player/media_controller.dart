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

  void prepareFromMediaId(String mediaId) {
    _channel.invokeMethod("prepareFromMediaId", mediaId);
  }

  void play() {
    _channel.invokeMethod("play");
  }

  void playFromMediaId(String mediaId) {
    _channel.invokeMethod("playFromMediaId", mediaId);
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

  void setPlayMode(PlayMode playMode) {
    //TODO
    _channel.invokeMethod("setPlayMode", playMode.index);
  }

  void setRepeatMode(int repeatMode) {
    _channel.invokeMethod("setRepeatMode", repeatMode);
  }

  void setShuffleMode(int shuffleMode) {
    _channel.invokeMethod("setShuffleMode", shuffleMode);
  }
}

enum PlayMode {
  sequence,
  shuffle,
  single,
}

class MediaController {
  final MethodChannel _channel;

  MediaController(this._channel);

  Future<bool> get isSessionReady => _channel.invokeMethod("isSessionReady");

  Future<PlaybackState> get playbackState =>
      _channel.invokeMethod("getPlaybackState").then((data) => PlaybackState.fromMap(data));

  Future<List<QueueItem>> get queue => _channel.invokeMethod("getQueue").then((data) {
        if (data is! List) return const [];
        return (data as List).map((t) => QueueItem.fromMap(t)).toList(growable: false);
      });

  Future<String> get queueTitle => _channel.invokeMethod("getQueueTitle");

  Future<PlaybackInfo> get playbackInfo => _channel.invokeMethod("getPlaybackInfo");

  Future<int> get repeatMode => _channel.invokeMethod("getRepeatMode");

  Future<int> get shuffleMode => _channel.invokeMethod("getShuffleMode");

  /// get the next media, could be null
  Future<MediaMetadata> getNext() async {
    final Map map = await _channel.invokeMethod("getNext");
    return MediaMetadata.fromMap(map);
  }

  /// get the previous media, could be null
  Future<MediaMetadata> getPrevious() async {
    final Map map = await _channel.invokeMethod("getPreivous");
    return MediaMetadata.fromMap(map);
  }
}

mixin MediaControllerCallback on ValueNotifier<MusicPlayerState> {
  @protected
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
      case "onPlayModeChanged":
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

  void onQueueTitleChanged(String title) {
    value = value.copyWith(queueTitle: title);
    notifyListeners();
  }

  void onShuffleModeChanged(int shuffleMode) {
    value = value.copyWith(shuffleMode: shuffleMode);
    notifyListeners();
  }
}
