import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:logging/logging.dart';
import 'package:music_player/music_player.dart';
import 'package:music_player/src/player/music_player.dart';

import 'internal/serialization.dart';

/// MusicPlayer for UI interaction.
class MusicPlayer extends Player {
  final log = Logger('MusicPlayer');

  static const _uiChannel = MethodChannel("tech.soit.quiet/player.ui");

  static MusicPlayer? _player;

  MusicPlayer._internal() : super() {
    _uiChannel.setMethodCallHandler(_handleRemoteCall);
    final initResult = _uiChannel.invokeMethod<bool>("init");
    _initCompleter.complete(initResult.then((value) => value ?? false));

    _queue.addListener(notifyListeners);
    _playMode.addListener(notifyListeners);
    _playbackState.addListener(notifyListeners);
    _metadata.addListener(notifyListeners);
  }

  factory MusicPlayer() {
    return _player ??= MusicPlayer._internal();
  }

  Future<void> setPlayQueue(PlayQueue queue) async {
    await _uiChannel.invokeMethod("setPlayQueue", queue.toMap());
  }

  Future<MusicMetadata> getNextMusic(MusicMetadata anchor) async {
    final Map map = await _uiChannel.invokeMethod("getNext", anchor.toMap());
    return MusicMetadata.fromMap(map);
  }

  Future<MusicMetadata> getPreviousMusic(MusicMetadata metadata) async {
    final Map map =
        await _uiChannel.invokeMethod("getPrevious", metadata.toMap());
    return MusicMetadata.fromMap(map);
  }

  @override
  ValueListenable<PlayQueue> get queueListenable => _queue;

  @override
  ValueListenable<PlaybackState> get playbackStateListenable => _playbackState;

  @override
  ValueListenable<PlayMode> get playModeListenable => _playMode;

  @override
  ValueListenable<MusicMetadata?> get metadataListenable => _metadata;

  PlayUriInterceptor? playUriInterceptor;
  ImageLoadInterceptor? imageLoadInterceptor;
  PlayQueueInterceptor? playQueueInterceptor;

  final ValueNotifier<PlayQueue> _queue =
      ValueNotifier(const PlayQueue.empty());
  final ValueNotifier<PlaybackState> _playbackState =
      ValueNotifier(const PlaybackState.none());
  final ValueNotifier<PlayMode> _playMode = ValueNotifier(PlayMode.sequence);
  final ValueNotifier<MusicMetadata?> _metadata = ValueNotifier(null);

  Future<dynamic> _handleRemoteCall(MethodCall call) async {
    log.fine("on MethodCall: ${call.method} args = ${call.arguments}");
    switch (call.method) {
      case 'onPlaybackStateChanged':
        _playbackState.value = createPlaybackState(call.arguments);
        break;
      case 'onMetadataChanged':
        _metadata.value = call.arguments == null
            ? null
            : MusicMetadata.fromMap(call.arguments);
        break;
      case 'onPlayQueueChanged':
        _queue.value = PlayQueue.fromMap(call.arguments);
        break;
      case 'onPlayModeChanged':
        _playMode.value = PlayMode(call.arguments as int?);
        break;
      case 'loadImage':
        if (imageLoadInterceptor != null) {
          return await imageLoadInterceptor!(
            MusicMetadata.fromMap(call.arguments),
          );
        }
        throw MissingPluginException();
      case 'getPlayUrl':
        if (playUriInterceptor != null) {
          final String? id = call.arguments['id'];
          final String? fallbackUrl = call.arguments['url'];
          return await playUriInterceptor!(id, fallbackUrl);
        }
        throw MissingPluginException();
      case "onPlayNextNoMoreMusic":
        if (playQueueInterceptor != null) {
          return await playQueueInterceptor!
              .onPlayNextNoMoreMusic(
                PlayMode(call.arguments["playMode"] as int?),
              )
              .toMap();
        }
        throw MissingPluginException();
      case "onPlayPreviousNoMoreMusic":
        if (playQueueInterceptor != null) {
          return playQueueInterceptor!
              .onPlayPreviousNoMoreMusic(
                PlayMode(call.arguments["playMode"] as int?),
              )
              .toMap();
        }
        throw MissingPluginException();
      default:
        throw UnimplementedError();
    }
  }

  TransportControls transportControls = TransportControls(_uiChannel);

  final Completer<bool> _initCompleter = Completer();

  Future<void> insertToNext(MusicMetadata metadata) {
    return _uiChannel.invokeMethod("insertToNext", metadata.toMap());
  }

  Future<void> playWithQueue(
    PlayQueue playQueue, {
    MusicMetadata? metadata,
  }) async {
    await setPlayQueue(playQueue);
    if (playQueue.isEmpty) {
      return;
    }
    metadata = metadata ?? playQueue.queue.first;
    log.fine("playFromMediaId : ${metadata.mediaId}");
    await transportControls.playFromMediaId(metadata.mediaId);
  }

  void removeMusicItem(MusicMetadata metadata) {}

  /// Check whether music service already running.
  Future<bool> isMusicServiceAvailable() {
    return _initCompleter.future;
  }
}

class MusicPlayerValue {
  final PlayQueue queue;

  final PlayMode playMode;

  final PlaybackState playbackState;

  final MusicMetadata? metadata;

  MusicPlayerValue({
    required this.queue,
    this.playMode = PlayMode.sequence,
    this.metadata,
    this.playbackState = const PlaybackState.none(),
  });

  static final _empty = MusicPlayerValue(
    queue: const PlayQueue.empty(),
    playMode: PlayMode.sequence,
    metadata: null,
    playbackState: const PlaybackState.none(),
  );

  factory MusicPlayerValue.none() {
    return _empty;
  }
}

extension _ToMap on Future<MusicMetadata?> {
  Future<Map<String, dynamic>?> toMap() {
    return then((value) => value?.toMap());
  }
}
