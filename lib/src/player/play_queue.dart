import 'music_metadata.dart';

class PlayQueue {
  final String queueId;

  /// nullable
  final String queueTitle;

  /// nullable
  final Map extras;

  final List<MusicMetadata> queue;

  PlayQueue({this.queueId, this.queueTitle, this.extras, this.queue}) : assert(queueId != null);
}
