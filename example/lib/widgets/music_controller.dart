import 'package:flutter/material.dart';
import 'package:music_player_example/player/player.dart';

class MusicControlBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bottomPadding = MediaQuery.of(context).viewPadding.bottom;
    var music = PlayerState.of(context).metadata?.getDescription();
    if (music == null) {
      return Container();
    }
    return InkWell(
      onTap: () {
        if (music != null) {
          debugPrint("on taped");
        }
      },
      child: Card(
        margin: const EdgeInsets.all(0),
        shape: const RoundedRectangleBorder(
            borderRadius:
                const BorderRadius.only(topLeft: const Radius.circular(4.0), topRight: const Radius.circular(4.0))),
        child: Container(
          height: 56 + bottomPadding,
          padding: EdgeInsets.only(bottom: bottomPadding),
          child: Row(
            children: <Widget>[
              Container(
                padding: const EdgeInsets.all(8),
                child: AspectRatio(
                  aspectRatio: 1,
                  child: ClipRRect(
                    borderRadius: const BorderRadius.all(Radius.circular(3)),
                    child: Container(color: Theme.of(context).primaryColor),
                  ),
                ),
              ),
              Expanded(
                child: DefaultTextStyle(
                  style: TextStyle(),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Spacer(),
                      Text(
                        music.title ?? "",
                        style: Theme.of(context).textTheme.body1,
                      ),
                      Padding(padding: const EdgeInsets.only(top: 2)),
                      DefaultTextStyle(
                        child: Text(music.subtitle ?? ""),
                        maxLines: 1,
                        style: Theme.of(context).textTheme.caption,
                      ),
                      Spacer(),
                    ],
                  ),
                ),
              ),
              _PauseButton(),
              IconButton(
                  tooltip: "当前播放列表",
                  icon: Icon(Icons.menu),
                  onPressed: () {
                    //TODO
                  }),
            ],
          ),
        ),
      ),
    );
  }
}

class _PauseButton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final playbackState = PlayerState.of(context).playbackState;
    if (playbackState.state == PlaybackState.STATE_PLAYING) {
      return IconButton(
          icon: Icon(Icons.pause),
          onPressed: () {
            PlayerWidget.transportControls(context).pause();
          });
    } else if (playbackState.state == PlaybackState.STATE_BUFFERING) {
      return Container(
        height: 24,
        width: 24,
        //to fit  IconButton min width 48
        margin: EdgeInsets.only(right: 12),
        padding: EdgeInsets.all(4),
        child: CircularProgressIndicator(),
      );
    } else if (playbackState.state == PlaybackState.STATE_NONE) {
      return Container();
    } else {
      return IconButton(
          icon: Icon(Icons.play_arrow),
          onPressed: () {
            PlayerWidget.transportControls(context).play();
          });
    }
  }
}
