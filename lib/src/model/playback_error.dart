import 'package:flutter/foundation.dart';

/// Error description when a non-recoverable playback failure occurs.
class PlaybackError {
  /// error type
  final ErrorType type;

  /// error message.
  final String message;

  PlaybackError({@required this.type, @required this.message});

  @override
  String toString() {
    return 'PlaybackError{type: $type, message: $message}';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PlaybackError && runtimeType == other.runtimeType && type == other.type && message == other.message;

  @override
  int get hashCode => type.hashCode ^ message.hashCode;
}

enum ErrorType {
  /// The error occurred when loading data.
  source,

  /// The error occurred when renderer data.
  renderer,

  /// the error is unknown.
  unknown,
}
