import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:music_player/music_player.dart';
import 'package:music_player/src/internal/meta.dart';
import 'package:music_player/src/internal/player_callback_adapter.dart';

import 'internal/serialization.dart';
import 'player/music_metadata.dart';
import 'player/play_queue.dart';
import 'player/playback_state.dart';
import 'player/transport_controls.dart';

const _uiChannel = MethodChannel("tech.soit.quiet/player.ui");

/// MusicPlayer for UI interaction.
class MusicPlayer extends ValueNotifier<MusicPlayerValue> with ChannelPlayerCallbackAdapter {
  static MusicPlayer _player;

  MusicPlayer._internal() : super(MusicPlayerValue.none()) {
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

  void setPlayQueue(@nonNull PlayQueue queue) {
    _uiChannel.invokeMethod("setPlayQueue", queue.toMap());
  }

  PlaybackState get playbackState => value.playbackState;

  MusicMetadata get metadata => value.metadata;

  PlayQueue get queue => value.queue;

  TransportControls transportControls = TransportControls(_uiChannel);

  void insertToNext(MusicMetadata metadata) {
    //TODO
  }

  void playWithQueue(PlayQueue playQueue, {MusicMetadata metadata}) {
    setPlayQueue(playQueue);
    if (playQueue.isEmpty) {
      return;
    }
    metadata = metadata ?? playQueue.queue.first;
    transportControls.playFromMediaId(metadata.mediaId);
  }

  void removeMusicItem(MusicMetadata metadata) {}
}

class MusicPlayerValue {
  @nonNull
  final PlayQueue queue;

  @nonNull
  final PlayMode playMode;

  @nonNull
  final PlaybackState playbackState;

  @nullable
  final MusicMetadata metadata;

  @nullable
  final MusicMetadata next;

  @nullable
  final MusicMetadata previous;

  MusicPlayerValue({
    this.queue,
    this.playMode,
    this.metadata,
    this.playbackState,
    this.next,
    this.previous,
  })  : assert(queue != null),
        assert(next == null || queue.queue.contains(next)),
        assert(previous == null || queue.queue.contains(previous));

  static final _empty = MusicPlayerValue(
    queue: PlayQueue.empty(),
    playMode: PlayMode.sequence,
    metadata: null,
    next: null,
    previous: null,
    playbackState: PlaybackState.none(),
  );

  factory MusicPlayerValue.none() {
    return _empty;
  }
}
