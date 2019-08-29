import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:music_player/src/model/media_metadata.dart';
import 'package:music_player/src/model/playback_info.dart';
import 'package:music_player/src/model/playback_state.dart';
import 'package:music_player/src/model/queue_item.dart';
import 'package:music_player/src/player/media_controller.dart';

// PlayerPlugin channel
const MethodChannel _channel = const MethodChannel('tech.soit.quiet/player');

class MusicPlayerState {
  final MediaMetadata metadata;
  final PlaybackInfo playbackInfo;
  final PlaybackState playbackState;
  final List<QueueItem> queue;
  final String queueTitle;
  final int repeatMode;
  final int ratingType;
  final int shuffleMode;

  const MusicPlayerState({
    this.metadata,
    this.playbackInfo,
    this.playbackState,
    this.queue,
    this.queueTitle,
    this.repeatMode,
    this.ratingType,
    this.shuffleMode,
  });

  MusicPlayerState copyWith({
    MediaMetadata metadata,
    PlaybackInfo playbackInfo,
    PlaybackState playbackState,
    List<QueueItem> queue,
    String queueTitle,
    int repeatMode,
    int ratingType,
    int shuffleMode,
  }) {
    return new MusicPlayerState(
      metadata: metadata ?? this.metadata,
      playbackInfo: playbackInfo ?? this.playbackInfo,
      playbackState: playbackState ?? this.playbackState,
      queue: queue ?? this.queue,
      queueTitle: queueTitle ?? this.queueTitle,
      repeatMode: repeatMode ?? this.repeatMode,
      ratingType: ratingType ?? this.ratingType,
      shuffleMode: shuffleMode ?? this.shuffleMode,
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
          shuffleMode: PlaybackState.SHUFFLE_MODE_NONE,
          repeatMode: PlaybackState.REPEAT_MODE_NONE,
        );
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
  }

  /// Transport controls for this player
  final TransportControls transportControls = TransportControls(_channel);

  /// Set the playlist of MusicPlayer
  void setPlayList(List<MediaMetadata> list) {}
}
