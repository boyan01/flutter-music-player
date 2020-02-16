import 'package:flutter/cupertino.dart';

import 'play_mode.dart';

class PlaybackState {
  final PlayerState state;

  final int position;

  final int bufferedPosition;

  final double speed;

  final error;

  final int updateTime;

  final PlayMode playMode;

  const PlaybackState({
    @required this.state,
    this.position,
    this.bufferedPosition,
    this.speed,
    this.error,
    this.updateTime,
    this.playMode,
  }) : assert(state != null);

  const PlaybackState.none()
      : this(
          state: PlayerState.None,
          position: 0,
          bufferedPosition: 0,
          speed: 1,
          updateTime: 0,
          playMode: PlayMode.sequence,
        );
}

enum PlayerState {
  None,
  Paused,
  Playing,
  Buffering,
  Error,
}
