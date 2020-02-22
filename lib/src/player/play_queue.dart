import 'package:flutter/cupertino.dart';

import 'music_metadata.dart';

class PlayQueue {
  final String queueId;

  /// nullable
  final String queueTitle;

  /// nullable
  final Map extras;

  final List<MusicMetadata> queue;

  bool get isEmpty => queue.isEmpty;

  const PlayQueue({@required this.queueId, @required this.queueTitle, this.extras, @required this.queue})
      : assert(queueId != null),
        assert(queueTitle != null),
        assert(queue != null);

  const PlayQueue.empty() : this(queueId: "empty", queueTitle: "empty", queue: const [], extras: const {});
}
