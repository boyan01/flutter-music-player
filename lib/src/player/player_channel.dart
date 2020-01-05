import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:music_player/src/model/media_metadata.dart';
import 'package:music_player/src/model/playback_info.dart';
import 'package:music_player/src/model/playback_state.dart';
import 'package:music_player/src/player/media_controller.dart';

import 'play_mode.dart';

// PlayerPlugin channel
const MethodChannel _channel = const MethodChannel('tech.soit.quiet/player');

class PlayList {
  final List<MediaMetadata> queue;

  // media id in shuffle
  final List<String> _shuffleQueue;
  final String queueTitle;
  final String queueId;

  PlayList({@required this.queue, this.queueTitle, @required this.queueId})
      : _shuffleQueue = queue.map((e) => e.mediaId).toList(),
        assert(queue != null),
        assert(queueId != null);

  const PlayList.empty() : this._internal(const [], const [], null, "");

  const PlayList._internal(this.queue, this._shuffleQueue, this.queueTitle, this.queueId);

  factory PlayList.fromMap(Map map) {
    return PlayList._internal(
      (map['queue'] as List).cast<Map>().map(((e) => MediaMetadata.fromMap(e))).toList() ?? const [],
      map['shuffleQueue'] as List<String> ?? const [],
      map['queueTitle'] as String,
      map['queueId'] as String ?? "",
    );
  }

  /// get [metadata] next [MediaMetadata]
  /// [shuffle] get next item in shuffle list
  MediaMetadata getNext(MediaMetadata metadata, {bool shuffle = false}) {
    assert(metadata != null);

    if (shuffle) {
      final next = _shuffleQueue.getNext(metadata.mediaId, true);
      // TODO more effective
      return queue.firstWhere((e) => e.mediaId == next);
    } else {
      return queue.getNext(metadata, true);
    }
  }

  MediaMetadata getPrevious(MediaMetadata metadata, {bool shuffle = false}) {
    assert(metadata != null);
    if (shuffle) {
      final next = _shuffleQueue.getPrevious(metadata.mediaId, true);
      // TODO more effective
      return queue.firstWhere((e) => e.mediaId == next);
    } else {
      return queue.getPrevious(metadata, true);
    }
  }
}

@visibleForTesting
extension GetNext<T> on List<T> {
  T getNext(T anchor, bool circle) {
    int index = this.indexOf(anchor);
    if (index == -1) {
      assert(() {
        throw "element$anchor not exist";
      }());
      return null;
    }
    index++;
    if (index == this.length) {
      if (!circle) return null;
      index = 0;
    }
    return this[index];
  }

  T getPrevious(T anchor, bool circle) {
    int index = this.indexOf(anchor);
    if (index == -1) {
      assert(() {
        throw "element$anchor not exist";
      }());
      return null;
    }
    index--;
    if (index == -1) {
      if (!circle) return null;
      index = this.length - 1;
    }
    return this[index];
  }
}

class MusicPlayerValue {
  /// current playing media metadata, could be null
  final MediaMetadata metadata;
  final PlaybackInfo playbackInfo;
  final PlaybackState playbackState;
  final PlayList playList;

  /// current queue play mode
  PlayMode get playMode => playbackState.playMode;

  const MusicPlayerValue({this.metadata, this.playbackInfo, this.playbackState, this.playList});

  MusicPlayerValue copyWith({
    PlaybackInfo playbackInfo,
    PlaybackState playbackState,
    PlayList playList,
  }) {
    return new MusicPlayerValue(
      metadata: this.metadata,
      playbackInfo: playbackInfo ?? this.playbackInfo,
      playbackState: playbackState ?? this.playbackState,
      playList: playList ?? this.playList,
    );
  }

  MusicPlayerValue setMetadata(MediaMetadata metadata) {
    return new MusicPlayerValue(
      metadata: metadata,
      playbackInfo: this.playbackInfo,
      playbackState: this.playbackState,
      playList: this.playList,
    );
  }

  const MusicPlayerValue.none()
      : this(
          metadata: null,
          playbackState: const PlaybackState.none(),
          playbackInfo: null,
          playList: const PlayList.empty(),
        );
}

typedef ValueCallback<T> = void Function<T>(T value);

class MusicPlayer extends ValueNotifier<MusicPlayerValue> with MediaControllerCallback {
  final void Function(MusicPlayer) onServiceConnected;

  MusicPlayer({this.onServiceConnected}) : super(const MusicPlayerValue.none()) {
    _listenNative();
  }

  PlaybackState get playbackState => value.playbackState;

  PlaybackInfo get playbackInfo => value.playbackInfo;

  PlayList get playList => value.playList;

  /// current playing metadata
  MediaMetadata get metadata => value.metadata;

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
    scheduleMicrotask(() async {
      final map = await _channel.invokeMethod<Map>("init");
      onPlayListChanged(PlayList.fromMap(map));
      if (onServiceConnected != null) {
        onServiceConnected(this);
      }
    });
  }

  /// Transport controls for this player
  final TransportControls transportControls = TransportControls(_channel);

  /// Set the playlist of MusicPlayer
  Future<void> setQueueAndId(List<MediaMetadata> list, String queueId, {String queueTitle}) async {
    assert(list != null);
    assert(queueId != null);
    await setPlayList(PlayList(queue: list, queueId: queueId, queueTitle: queueTitle));
  }

  Future<void> setPlayList(PlayList playList) async {
    assert(playList != null);
    await _channel.invokeMethod("updatePlayList", {
      "queue": playList.queue.map((e) => e.toMap()).toList(),
      "queueTitle": playList.queueTitle,
      "queueId": playList.queueId,
    });
  }

  Future<void> playWithList(PlayList playList, {MediaMetadata metadata}) async {
    assert(playList != null);
    if (metadata != null) {
      assert(playList.queue.contains(metadata));
    }
    await setPlayList(playList);
    if (metadata != null) {
      await transportControls.playFromMediaId(metadata.mediaId);
    } else {
      await transportControls.play();
    }
  }

  /// Add to [playList] after [MusicPlayerValue.metadata]
  Future<void> insertToNext(MediaMetadata metadata) async {
    //TODO
  }

  void removeQueueItem(String mediaId) {
    //TODO
  }
}
