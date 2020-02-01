class PlaybackState {
  final State state;

  final int position;

  final int bufferedPosition;

  final double speed;

  final error;

  final int updateTime;

  PlaybackState({
    this.state,
    this.position,
    this.bufferedPosition,
    this.speed,
    this.error,
    this.updateTime,
  });
}

enum State {
  None,
  Paused,
  Playing,
  Buffering,
  Error,
}
