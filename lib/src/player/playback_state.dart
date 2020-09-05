import 'package:flutter/foundation.dart';
import 'package:music_player/music_player.dart';
import 'package:system_clock/system_clock.dart';

class PlaybackState {
  final PlayerState state;

  final int position;

  final int bufferedPosition;

  final double speed;

  final PlaybackError error;

  final int updateTime;

  ///
  /// Get the real position in current time stamp.
  ///
  /// discuss: https://github.com/boyan01/flutter-music-player/issues/1
  ///
  int get computedPosition {
    var append = state == PlayerState.Playing ? (SystemClock.uptime().inMilliseconds - updateTime) : 0;
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

  @override
  String toString() {
    return 'PlaybackState{state: $state, position: $position, bufferedPosition: $bufferedPosition, speed: $speed, error: $error, updateTime: $updateTime}';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PlaybackState &&
          runtimeType == other.runtimeType &&
          state == other.state &&
          position == other.position &&
          bufferedPosition == other.bufferedPosition &&
          speed == other.speed &&
          error == other.error &&
          updateTime == other.updateTime;

  @override
  int get hashCode =>
      state.hashCode ^
      position.hashCode ^
      bufferedPosition.hashCode ^
      speed.hashCode ^
      error.hashCode ^
      updateTime.hashCode;
}

enum PlayerState {
  None,
  Paused,
  Playing,
  Buffering,
  Error,
}
