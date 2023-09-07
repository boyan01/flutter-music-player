import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:music_player_example/page_play_queue.dart';
import 'package:overlay_support/overlay_support.dart';

import 'player/music_metadata.dart';
import 'player/player.dart';
import 'player/player_bottom_controller.dart';

final medias = [
  MusicMetadata(
    title: "Zhu Lin Jian",
    subtitle: "Zhu Lin Jian - SanWu marblue",
    mediaId: "bamboo",
    mediaUri: "asset:///tracks/bamboo.mp3",
    iconUri: "https://via.placeholder.com/150/FFCA28/000000/?text=bamboo",
  ),
  MusicMetadata(
    title: "Rise",
    subtitle: "Rise - The Glitch Mob",
    mediaId: "rise",
    mediaUri: "asset:///tracks/rise.mp3",
    iconUri: "https://via.placeholder.com/150/4CAF50/FFFFFF/?text=Rise",
  ),
  MusicMetadata(
    title: "Cang",
    subtitle: "Cang - xu meng yuan",
    mediaId: "hide",
    mediaUri: "asset:///tracks/hide.mp3",
    iconUri: "https://via.placeholder.com/150/03A9F4/000000/?text=Cang",
  ),
];

final playQueueList = [
  PlayQueue(queueTitle: "Simple Test", queueId: "test1", queue: medias),
  PlayQueue(
      queueTitle: "Auto Fetch Test",
      queueId: "fm",
      queue: medias.getRange(0, 1).toList()),
  PlayQueue(queueTitle: "Failed to Play", queueId: "test_failed", queue: [
    MusicMetadata(
      title: "Cang",
      subtitle: "Cang - xu meng yuan",
      mediaId: "hide",
      mediaUri: "asset:///tracks/file_not_exists.mp3",
    ),
  ])
];

void main() {
  Logger.root.onRecord.listen((record) {
    print('${record.level.name}: ${record.time}: ${record.message}');
  });
  runApp(ExampleApp());
}

class ExampleApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return PlayerWidget(
      child: OverlaySupport(
        child: MaterialApp(
          home: ExamplePage(),
        ),
      ),
    );
  }
}

class ExamplePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Flutter Music Player Example")),
      body: Column(
        children: <Widget>[
          Expanded(child: _PlayQueueListView(playQueueList: playQueueList)),
          PlayerBottomController(),
        ],
      ),
    );
  }
}

class _PlayQueueListView extends StatelessWidget {
  final List<PlayQueue> playQueueList;

  const _PlayQueueListView({Key key, this.playQueueList}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
        itemCount: playQueueList.length,
        itemBuilder: (context, index) {
          final queue = playQueueList[index];
          return ListTile(
            title: Text(queue.queueTitle),
            onTap: () {
              Navigator.push(
                  context,
                  new MaterialPageRoute(
                      builder: (context) => PagePlayQueue(queue: queue)));
            },
          );
        });
  }
}
