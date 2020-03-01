import 'package:flutter/cupertino.dart';

class PlaybackState {
  final PlayerState state;

  final int position;

  final int bufferedPosition;

  final double speed;

  final error;

  final int updateTime;

  int get computedPosition {
    final append = state == PlayerState.Playing ? (DateTime.now().millisecondsSinceEpoch - updateTime) : 0;
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
