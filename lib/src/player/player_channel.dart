import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:music_player/src/model/media_metadata.dart';
import 'package:music_player/src/model/playback_info.dart';
import 'package:music_player/src/model/playback_state.dart';
import 'package:music_player/src/player/media_controller.dart';

// PlayerPlugin channel
const MethodChannel _channel = const MethodChannel('tech.soit.quiet/player');

class MusicPlayerState {
  final MediaMetadata metadata;
  final PlaybackInfo playbackInfo;
  final PlaybackState playbackState;
  final List<MediaMetadata> queue;
  final String queueTitle;
  final PlayMode playMode;
  final int ratingType;
  final String queueId;

  const MusicPlayerState(
      {this.metadata,
      this.playbackInfo,
      this.playbackState,
      this.queue,
      this.queueTitle,
      this.ratingType,
      this.queueId,
      this.playMode});

  MusicPlayerState copyWith({
    MediaMetadata metadata,
    PlaybackInfo playbackInfo,
    PlaybackState playbackState,
    List<MediaMetadata> queue,
    String queueTitle,
    int repeatMode,
    int ratingType,
    int shuffleMode,
    String queueId,
  }) {
    return new MusicPlayerState(
      metadata: metadata ?? this.metadata,
      playbackInfo: playbackInfo ?? this.playbackInfo,
      playbackState: playbackState ?? this.playbackState,
      queue: queue ?? this.queue,
      queueTitle: queueTitle ?? this.queueTitle,
      ratingType: ratingType ?? this.ratingType,
      playMode: playMode ?? this.playMode,
      queueId: queueId ?? this.queueId,
    );
  }

  const MusicPlayerState.none()
      : this(
            metadata: null,
            playbackState: const PlaybackState.none(),
            playbackInfo: null,
            queue: const [],
            queueTitle: "NONE",
            ratingType: 0,
            playMode: PlayMode.sequence);

  Map<String, dynamic> toMap() {
    return {
      'metadata': this.metadata.toMap(),
      'playbackInfo': this.playbackInfo,
      'playbackState': this.playbackState.toMap(),
      'queue': this.queue,
      'queueTitle': this.queueTitle,
      'playMode': this.playMode.index,
      'ratingType': this.ratingType,
    };
  }

  factory MusicPlayerState.fromMap(Map map) {
    if (map == null) return null;
    return new MusicPlayerState(
      metadata: MediaMetadata.fromMap(map['metadata']),
      playbackInfo: null,
      playbackState: PlaybackState.fromMap(map['playbackState']),
      queue: (map['queue'] as List)?.map((it) => MediaMetadata.fromMap(it))?.toList() ?? const [],
      queueTitle: map['queueTitle'] as String,
      //TODO
      playMode: PlayMode.sequence,
      ratingType: map['ratingType'] as int,
    );
  }
}

class MusicPlayer extends ValueNotifier<MusicPlayerState> with MediaControllerCallback {
  MusicPlayer() : super(const MusicPlayerState.none()) {
    _listenNative();
  }

  void _listenNative() {
    _channel.setMethodCallHandler((call) async {
      debugPrint("call from native: ${call.method} , arg = ${call.arguments}");
      try {
        if (!handleMediaControllerCallbackMethod(call)) {
          //un implement
        }
      } catch (e, trace) {
        debugPrint(e.toString());
        debugPrint(trace.toString());
      }
    });
    _channel.invokeMethod("init");
  }

  /// Transport controls for this player
  final TransportControls transportControls = TransportControls(_channel);

  final MediaController mediaController = MediaController(_channel);

  /// Set the playlist of MusicPlayer
  Future<void> setPlayList(List<MediaMetadata> list, String queueId, {String queueTitle}) async {
    assert(list != null);
    assert(queueId != null);
    value = value.copyWith(queue: list, queueTitle: queueTitle, queueId: queueId);
    await _channel.invokeMethod("updatePlayList", {
      "queue": list.map((item) => item.toMap()).toList(),
      "queueTitle": queueTitle,
      "queueId": queueId,
    });
  }
}
