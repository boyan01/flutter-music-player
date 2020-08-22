import 'package:flutter/cupertino.dart';
import 'package:system_clock/system_clock.dart';

class PlaybackState {
  final PlayerState state;

  final int position;

  final int bufferedPosition;

  final double speed;

  final error;

  final int updateTime;

  ///
  /// Get the real position in current time stamp.
  ///
  /// discuss: https://github.com/boyan01/flutter-music-player/issues/1
  ///
  int get computedPosition {
    var append = state == PlayerState.Playing ? (SystemClock.uptimeMills - updateTime) : 0;
    append = (append * speed).toInt();
    return position + append;
  }

  const PlaybackState({
    @required this.state,
    this.position,
    this.bufferedPosition,
    this.speed,
    this.error,
    this.updateTime,
  }) : assert(state != null);

  const PlaybackState.none()
      : this(
          state: PlayerState.None,
          position: 0,
          bufferedPosition: 0,
          speed: 1,
          updateTime: 0,
        );
}

enum PlayerState {
  None,
  Paused,
  Playing,
  Buffering,
  Error,
}
