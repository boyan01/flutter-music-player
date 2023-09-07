import 'package:music_player_example/player/player.dart';

class AppPlayQueue implements PlayQueue {
  final String queueId;

  final List<PlayItem> queue;

  bool get isEmpty => queue.isEmpty;

  const AppPlayQueue({
    required this.queueId,
    required this.queue,
  });

  const AppPlayQueue.empty()
      : this(
          queueId: "empty",
          queue: const [],
        );

  @override
  Future<PlayItem?> getNext(PlayItem? current) async {
    if (isEmpty) {
      return null;
    }
    if (current == null) {
      return queue.first;
    }
    final index = queue.indexOf(current);
    if (index == -1) {
      return null;
    }
    if (index == queue.length - 1) {
      return null;
    }
    return queue[index + 1];
  }

  @override
  Future<PlayItem?> getPrevious(PlayItem? current) async {
    if (isEmpty) {
      return null;
    }
    if (current == null) {
      return queue.first;
    }
    final index = queue.indexOf(current);
    if (index == -1) {
      return null;
    }
    if (index == 0) {
      return null;
    }
    return queue[index - 1];
  }
}
