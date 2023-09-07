import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:music_player/src/utils/extension.dart';
import 'package:system_clock/system_clock.dart';

import 'utils/logger.dart';

abstract class PlayItem {
  Future<String> playerUrl();

  PlayItemMetadata? metadata() => null;
}

class PlayItemMetadata {
  final String title;
  final String artist;
  final String album;
  final String albumArtUrl;
  final Duration duration;

  PlayItemMetadata(
    this.title,
    this.artist,
    this.album,
    this.albumArtUrl,
    this.duration,
  );
}

const _methodChannel = MethodChannel('tech.soit.quiet/player');

class PlayerChannel {
  void initialize() {
    _methodChannel.setMethodCallHandler((call) async {
      try {
        return await _handleMethodCall(call);
      } catch (error, stacktrace) {
        logger.severe('Error in method call handler', error, stacktrace);
      }
    });
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onPlayerStateChanged':
        final playerId = call.arguments['id'] as int;
        final state = call.arguments['state'] as int;
        final player =
            _audioPlayers.firstWhereOrNull((it) => it._id == playerId);
        if (player == null) {
          logger.warning('player not found. $playerId');
          return;
        }
        final stateE = PlayerState.values[state];
        player._state.value = stateE;
        break;
      case 'onPositionChanged':
        final playerId = call.arguments['id'] as int;
        final currentPosition = call.arguments['currentPosition'] as int;
        final duration = call.arguments['duration'] as int;
        final updateTime = call.arguments['updateTime'] as int;
        final player =
            _audioPlayers.firstWhereOrNull((it) => it._id == playerId);
        if (player == null) {
          logger.warning('player not found. $playerId');
          return;
        }
        player._positionUpdateTime = Duration(milliseconds: updateTime);
        player._position = Duration(milliseconds: currentPosition);
        player._duration = Duration(milliseconds: duration);
        break;
      case 'onPlayerError':
        final playerId = call.arguments['id'] as int;
        final errorCode = call.arguments['errorCode'] as int;
        final message = call.arguments['message'] as String;
        final player =
            _audioPlayers.firstWhereOrNull((it) => it._id == playerId);
        if (player == null) {
          logger.warning('player not found. $playerId');
          return;
        }
        player._state.value = PlayerState.idle;
        player._error.value = PlaybackException(errorCode, message);
        break;

      case 'onPlayWhenReadyChanged':
        final playerId = call.arguments['id'] as int;
        final playWhenReady = call.arguments['playWhenReady'] as bool;
        final player =
            _audioPlayers.firstWhereOrNull((it) => it._id == playerId);
        if (player == null) {
          logger.warning('player not found. $playerId');
          return;
        }
        player._playWhenReady.value = playWhenReady;
        break;
      default:
        throw MissingPluginException();
    }
  }
}

enum PlayerState {
  idle,
  buffering,
  ready,
  ended,
}

class PlaybackException implements Exception {
  PlaybackException(this.errorCode, this.message);

  final String message;

  // TODO(bin): define error code
  final int errorCode;

  @override
  String toString() => 'PlaybackException($errorCode, $message)';
}

final _audioPlayers = <AudioPlayer>[];

class AudioPlayer {
  AudioPlayer._private(this._channel, this.url) {
    scheduleMicrotask(() async {
      try {
        final playerId = await _channel.invokeMethod<int>('createPlayer');
        if (playerId == null || playerId < 0) {
          logger.warning('create player failed. $playerId');
          _error.value = PlaybackException(0, 'create player failed');
          return;
        }
        _id = playerId;
      } catch (error, stacktrace) {
        logger.severe('Error in create player', error, stacktrace);
        _state.value = PlayerState.idle;
        _error.value = PlaybackException(0, error.toString());
      } finally {
        _initialized.complete();
      }
    });
  }

  factory AudioPlayer(String url) {
    final player = AudioPlayer._private(_methodChannel, url);
    _audioPlayers.add(player);
    return player;
  }

  final _state = ValueNotifier(PlayerState.idle);

  final _error = ValueNotifier<PlaybackException?>(null);

  var _duration = Duration.zero;

  var _position = Duration.zero;

  var _positionUpdateTime = Duration.zero;

  final String url;

  final MethodChannel _channel;

  final _initialized = Completer<void>();

  int _id = -1;

  bool get _valid => _id >= 0;

  final _playWhenReady = ValueNotifier(false);

  bool get playWhenReady => _playWhenReady.value;

  ChangeNotifier get playWhenReadyNotifier => _playWhenReady;

  PlayerState get state => _state.value;

  ChangeNotifier get stateNotifier => _state;

  PlaybackException? get playerError => _error.value;

  ChangeNotifier get errorNotifier => _error;

  Duration get currentPosition {
    final isPlaying = state == PlayerState.ready && playWhenReady;
    if (!isPlaying) {
      return _position;
    }
    if (_positionUpdateTime <= Duration.zero) {
      logger.warning('position update time not set');
      return _position;
    }
    final offset = SystemClock.elapsedRealtime() - _positionUpdateTime;
    return _position + offset;
  }

  Duration get duration => _duration;

  set playWhenReady(bool playWhenReady) {
    _playWhenReady.value = playWhenReady;
    if (!_valid) {
      return;
    }
    _channel.invokeMethod('setPlayWhenReady', {
      'id': _id,
      'playWhenReady': playWhenReady,
    });
  }

  Future<void> setPlayUrl(String url) async {
    if (!_valid) {
      logger.warning('player not initialized');
      return;
    }
    await _channel.invokeMethod('setPlayUrl', {
      'id': _id,
      'url': url,
    });
  }

  Future<void> prepare() async {
    await _initialized.future;
    if (!_valid) {
      logger.warning('player not initialized');
      return;
    }
    await _channel.invokeMethod('prepare', {
      'id': _id,
      'playWhenReady': _playWhenReady,
    });
  }

  Future<void> seekTo(Duration position) async {
    if (!_valid) {
      logger.warning('player not initialized');
      return;
    }
    await _channel.invokeMethod('seekTo', {
      'id': _id,
      'position': position.inMilliseconds,
    });
  }

  Future<Duration> get bufferedPosition async {
    if (!_valid) {
      logger.warning('player not initialized');
      return Duration.zero;
    }
    final position = await _channel.invokeMethod<int>('getBufferedPosition', {
      'id': _id,
    });
    return Duration(milliseconds: position ?? 0);
  }

  Future<void> dispose() async {
    if (!_valid) {
      logger.warning('player not initialized');
      return;
    }
    _state.dispose();
    _error.dispose();
    _playWhenReady.dispose();
    _audioPlayers.remove(this);
    final id = _id;
    _id = -1;
    await _channel.invokeMethod('dispose', {'id': id});
  }
}

@immutable
abstract class PlayQueue {
  Future<PlayItem?> getNext(PlayItem? current);

  Future<PlayItem?> getPrevious(PlayItem? current);
}

class MusicPlayer {
  PlayQueue? _queue;
  AudioPlayer? _player;

  MusicPlayer();

  PlayItem? _current;

  final _state = ValueNotifier(PlayerState.idle);
  final _error = ValueNotifier<PlaybackException?>(null);
  final _playWhenReady = ValueNotifier(false);

  PlayerState get state => _state.value;

  ChangeNotifier get stateNotifier => _state;

  PlaybackException? get playerError => _error.value;

  ChangeNotifier get errorNotifier => _error;

  Duration get currentPosition => _player?.currentPosition ?? Duration.zero;

  Duration get duration => _player?.duration ?? Duration.zero;

  bool get playWhenReady => _playWhenReady.value;

  ChangeNotifier get playWhenReadyNotifier => _playWhenReady;

  set playWhenReady(bool playWhenReady) {
    _playWhenReady.value = playWhenReady;
    _player?.playWhenReady = playWhenReady;
  }

  Future<void> _play(PlayItem item) async {
    if (_player != null) {
      unawaited(_player!.dispose());
    }
    _current = item;
    _state.value = PlayerState.idle;
    final audioPlayer = AudioPlayer(await item.playerUrl());
    _player = audioPlayer;
    audioPlayer.playWhenReady = playWhenReady;
    audioPlayer.stateNotifier.addListener(() {
      final state = audioPlayer.state;
      if (state == PlayerState.ended) {
        skipToNext();
      }
      _state.value = state;
    });
    audioPlayer.playWhenReadyNotifier.addListener(() {
      _playWhenReady.value = audioPlayer.playWhenReady;
    });
    audioPlayer.errorNotifier.addListener(() {
      _error.value = audioPlayer.playerError;
      _state.value = PlayerState.idle;
      _player?.dispose();
      _player = null;
    });
    await audioPlayer.prepare();
    audioPlayer.playWhenReady = true;
  }

  Future<void> skipToNext() async {
    if (_queue == null) {
      logger.warning('queue not set');
      return;
    }
    final item = await _queue!.getNext(_current);
    if (item == null) {
      logger.info('no next item');
      return;
    }
    await _play(item);
  }

  Future<void> skipToPrevious() async {
    if (_queue == null) {
      logger.warning('queue not set');
      return;
    }
    final item = await _queue!.getPrevious(_current);
    if (item == null) {
      logger.info('no previous item');
      return;
    }
    await _play(item);
  }

  Future<void> setQueue(PlayQueue queue) async {
    _queue = queue;
    await skipToNext();
  }

  Future<void> dispose() async {
    if (_player != null) {
      await _player!.dispose();
    }
  }
}
