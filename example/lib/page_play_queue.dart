import 'package:flutter/material.dart';
import 'package:music_player_example/player/player.dart';
import 'package:music_player_example/player/player_bottom_controller.dart';

class PagePlayQueue extends StatelessWidget {
  final PlayQueue queue;

  const PagePlayQueue({Key key, this.queue}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text("Queue: ${queue.queueTitle}"),
        ),
        body: Column(
          children: <Widget>[
            Expanded(
                child: ListView.builder(
                    itemCount: queue.queue.length,
                    itemBuilder: (context, index) {
                      final metadata = queue.queue[index];
                      return _MusicTitle(metadata: metadata, queue: queue);
                    })),
            PlayerBottomController(),
          ],
        ));
  }
}

class _MusicTitle extends StatelessWidget {
  final PlayQueue queue;

  final MusicMetadata metadata;

  const _MusicTitle({Key key, @required this.metadata, @required this.queue}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final playerValue = context.listenPlayerValue;
    bool playing = playerValue.queue.queueId == queue.queueId && playerValue.metadata.mediaId == metadata.mediaId;
    return ListTile(
      leading: playing ? const Icon(Icons.volume_up) : const Icon(Icons.music_note),
      title: Text(metadata.title),
      subtitle: Text(metadata.subtitle),
      onTap: () {
        context.player.playWithQueue(queue, metadata: metadata);
      },
    );
  }
}
