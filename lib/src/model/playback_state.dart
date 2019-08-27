/// Playback state of MusicPlayer.
class PlaybackState {
  static const int STATE_NONE = 0;
  static const int STATE_STOPPED = 1;
  static const int STATE_PAUSED = 2;
  static const int STATE_PLAYING = 3;
  static const int STATE_FAST_FORWARDING = 4;
  static const int STATE_REWINDING = 5;
  static const int STATE_BUFFERING = 6;
  static const int STATE_ERROR = 7;
  static const int STATE_CONNECTING = 8;
  static const int STATE_SKIPPING_TO_PREVIOUS = 9;
  static const int STATE_SKIPPING_TO_NEXT = 10;
  static const int STATE_SKIPPING_TO_QUEUE_ITEM = 11;

  static const int PLAYBACK_POSITION_UNKNOWN = -1;

  static const int REPEAT_MODE_INVALID = -1;
  static const int REPEAT_MODE_NONE = 0;
  static const int REPEAT_MODE_ONE = 1;
  static const int REPEAT_MODE_ALL = 2;
  static const int REPEAT_MODE_GROUP = 3;

  static const int SHUFFLE_MODE_INVALID = -1;
  static const int SHUFFLE_MODE_NONE = 0;
  static const int SHUFFLE_MODE_ALL = 1;
  static const int SHUFFLE_MODE_GROUP = 2;

  static const int ERROR_CODE_UNKNOWN_ERROR = 0;
  static const int ERROR_CODE_APP_ERROR = 1;
  static const int ERROR_CODE_NOT_SUPPORTED = 2;
  static const int ERROR_CODE_AUTHENTICATION_EXPIRED = 3;
  static const int ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED = 4;
  static const int ERROR_CODE_CONCURRENT_STREAM_LIMIT = 5;
  static const int ERROR_CODE_PARENTAL_CONTROL_RESTRICTED = 6;
  static const int ERROR_CODE_NOT_AVAILABLE_IN_REGION = 7;
  static const int ERROR_CODE_CONTENT_ALREADY_PLAYING = 8;
  static const int ERROR_CODE_SKIP_LIMIT_REACHED = 9;
  static const int ERROR_CODE_ACTION_ABORTED = 10;
  static const int ERROR_CODE_END_OF_QUEUE = 11;

  /// Get the current state of playback. One of the following:
  ///
  ///  [STATE_NONE]
  ///  [STATE_STOPPED]
  ///  [STATE_PLAYING]
  ///  [STATE_PAUSED]
  ///  [STATE_FAST_FORWARDING]
  ///  [STATE_REWINDING]
  ///  [STATE_BUFFERING]
  ///  [STATE_ERROR]
  ///  [STATE_CONNECTING]
  ///  [STATE_SKIPPING_TO_PREVIOUS]
  ///  [STATE_SKIPPING_TO_NEXT]
  ///  [STATE_SKIPPING_TO_QUEUE_ITEM]
  ///
  final int state;
  final int position;
  final int bufferedPosition;
  final double speed;
  final int actions;

  /// Get the error code. This should be set when the state is [STATE_ERROR].
  ///
  /// [ERROR_CODE_UNKNOWN_ERROR]
  /// [ERROR_CODE_APP_ERROR]
  /// [ERROR_CODE_NOT_SUPPORTED]
  /// [ERROR_CODE_AUTHENTICATION_EXPIRED]
  /// [ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED]
  /// [ERROR_CODE_CONCURRENT_STREAM_LIMIT]
  /// [ERROR_CODE_PARENTAL_CONTROL_RESTRICTED]
  /// [ERROR_CODE_NOT_AVAILABLE_IN_REGION]
  /// [ERROR_CODE_CONTENT_ALREADY_PLAYING]
  /// [ERROR_CODE_SKIP_LIMIT_REACHED]
  /// [ERROR_CODE_ACTION_ABORTED]
  /// [ERROR_CODE_END_OF_QUEUE]
  ///
  final int errorCode;

  /// Get the user readable optional error message. This may be set when the state is [STATE_ERROR]
  final String errorMessage;
  final int updateTime;
  final int activeItemId;
  final Map extras;

  Map<String, dynamic> toMap() {
    return {
      'state': this.state,
      'position': this.position,
      'bufferedPosition': this.bufferedPosition,
      'speed': this.speed,
      'actions': this.actions,
      'errorCode': this.errorCode,
      'errorMessage': this.errorMessage,
      'updateTime': this.updateTime,
      'activeItemId': this.activeItemId,
      'extras': this.extras,
    };
  }

  factory PlaybackState.fromMap(Map<String, dynamic> map) {
    return new PlaybackState(
      state: map['state'] as int,
      position: map['position'] as int,
      bufferedPosition: map['bufferedPosition'] as int,
      speed: map['speed'] as double,
      actions: map['actions'] as int,
      errorCode: map['errorCode'] as int,
      errorMessage: map['errorMessage'] as String,
      updateTime: map['updateTime'] as int,
      activeItemId: map['activeItemId'] as int,
      extras: map['extras'] as Map,
    );
  }

  @override
  String toString() {
    return 'PlaybackState{state: $state, position: $position, bufferedPosition: $bufferedPosition, speed: $speed, actions: $actions, errorCode: $errorCode, errorMessage: $errorMessage, updateTime: $updateTime, activeItemId: $activeItemId, extras: $extras}';
  }

  const PlaybackState({
    this.state,
    this.position,
    this.bufferedPosition,
    this.speed,
    this.actions,
    this.errorCode,
    this.errorMessage,
    this.updateTime,
    this.activeItemId,
    this.extras,
  });
}
