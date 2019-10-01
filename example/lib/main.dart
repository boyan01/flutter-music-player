import 'package:flutter/material.dart';
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
          Expanded(child: _ExampleMusicList()),
          MusicControlBar(),
        ],
      ),
    );
  }
}

void playerBackgroundService() {
  runBackgroundService(
    playUriInterceptor: (id, url) {
      if (id == "hide") {
        return "asset:///flutter_assets/tracks/rise.mp3";
      }
      return url;
    },
  );
}

class _ExampleMusicList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ListView.builder(
        itemCount: medias.length,
        itemBuilder: (context, index) {
          final item = medias[index].getDescription();
          return ListTile(
            title: Text(item.title),
            subtitle: Text(item.subtitle ?? ""),
            trailing: Icon(Icons.play_circle_outline),
            onTap: () {
              PlayerWidget.transportControls(context).playFromMediaId(item.mediaId, medias, "Example");
            },
          );
        });
  }
}
