import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:overlay_support/overlay_support.dart';

import 'player/player.dart';
import 'widgets/music_controller.dart';

void main() => runApp(ExampleApp());

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
      body: Column(
        children: <Widget>[
          Expanded(child: _ExampleMusicList(listId: "playlist001")),
          MusicControlBar(),
        ],
      ),
    );
  }
}

void playerBackgroundService() {
  runBackgroundService(playUriInterceptor: (mediaId, fallbackUrl) async {
    debugPrint("get media play uri : $mediaId , $fallbackUrl");
    if (mediaId == 'rise') return "asset:///tracks/rise.mp3";
    return fallbackUrl;
  }, imageLoadInterceptor: (metadata) async {
    debugPrint("load image for ${metadata.mediaId} , ${metadata.title}");
    if (metadata.mediaId == "bamboo") {
      final data = await rootBundle.load("images/bamboo.jpg");
      return Uint8List.view(data.buffer);
    }
    return null;
  });
}

class _ExampleMusicList extends StatelessWidget {
  final String listId;

  const _ExampleMusicList({Key key, @required this.listId})
      : assert(listId != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
        itemCount: medias.length,
        itemBuilder: (context, index) {
          final item = medias[index];
          return ListTile(
            title: Text(item.title),
            subtitle: Text(item.subtitle ?? ""),
            trailing: Icon(Icons.play_circle_outline),
            onTap: () async {
              final player = PlayerWidget.player(context);
              player.playWithQueue(PlayQueue(queue: medias, queueId: listId, queueTitle: "Exmaple PlayList"),
                  metadata: medias[index]);
            },
          );
        });
  }
}
