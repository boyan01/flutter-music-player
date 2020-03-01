import 'package:flutter/material.dart';

import 'player/player.dart';
import 'widgets/music_controller.dart';

class PagePlaying extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(context.listenPlayerValue.metadata.title)),
      body: SafeArea(
        child: Column(children: <Widget>[
          Expanded(child: Center(child: Icon(Icons.music_video, size: 120))),
          _PlayingInformation(),
          ProgressTrackingContainer(
            player: context.player,
            builder: (context) => _PlayingProgress(),
          ),
          ButtonBar(
            mainAxisSize: MainAxisSize.max,
            alignment: MainAxisAlignment.center,
            layoutBehavior: ButtonBarLayoutBehavior.constrained,
            children: <Widget>[
              RepeatModelButton(),
              IconButton(
                  icon: Icon(Icons.skip_previous),
                  onPressed: () {
                    PlayerWidget.transportControls(context).skipToPrevious();
                  }),
              PlayPauseButton(),
              IconButton(
                  icon: Icon(Icons.skip_next),
                  onPressed: () {
                    PlayerWidget.transportControls(context).skipToNext();
                  }),
            ],
          )
        ]),
      ),
    );
  }
}

class _PlayingInformation extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Text("current playing : ${context.listenPlayerValue.metadata.title} "),
        FutureBuilder<MusicMetadata>(
          future: context.player.getPreviousMusic(context.listenPlayerValue.metadata),
          builder: (context, snapshot) {
            return Text("previous : ${snapshot.data?.title}");
          },
        ),
        FutureBuilder<MusicMetadata>(
          future: context.player.getNextMusic(context.listenPlayerValue.metadata),
          builder: (context, snapshot) {
            return Text("next : ${snapshot.data?.title}");
          },
        ),
      ],
    );
  }
}

class _PlayingProgress extends StatefulWidget {
  @override
  _PlayingProgressState createState() => _PlayingProgressState();
}

class _PlayingProgressState extends State<_PlayingProgress> {
  bool _isUserTracking = false;
  bool _isPausedByTracking = false;
  double _userTrackingPosition = 0.0;

  @override
  Widget build(BuildContext context) {
    final playbackState = context.listenPlayerValue.playbackState;
    final metadata = context.listenPlayerValue.metadata;
    final int position = _isUserTracking ? _userTrackingPosition.round() : playbackState.computedPosition;
    return SliderTheme(
      data: SliderThemeData(thumbShape: RoundSliderThumbShape(enabledThumbRadius: 6)),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 16),
        child: Row(
          mainAxisSize: MainAxisSize.max,
          children: <Widget>[
            Text(position.formatAsTimeStamp()),
            Expanded(
              child: Slider(
                value: position.toDouble().clamp(0.0, metadata.duration.toDouble()),
                max: metadata.duration.toDouble(),
                onChangeStart: (value) {
                  setState(() {
                    _isUserTracking = true;
                    if (playbackState.state == PlayerState.Playing) {
                      _isPausedByTracking = true;
                      context.transportControls.pause();
                    }
                  });
                },
                onChanged: (value) {
                  _userTrackingPosition = value;
                  setState(() {});
                },
                onChangeEnd: (value) {
                  setState(() {
                    _isUserTracking = false;
                    if (_isPausedByTracking) {
                      context.transportControls.play();
                    }
                    context.transportControls.seekTo(value.toInt());
                  });
                },
              ),
            ),
            Text(metadata.duration.formatAsTimeStamp()),
          ],
        ),
      ),
    );
  }
}

extension _TimeStampFormatter on int {
  String formatAsTimeStamp() {
    int seconds = (this / 1000).truncate();
    int minutes = (seconds / 60).truncate();

    String minutesStr = (minutes % 60).toString().padLeft(2, '0');
    String secondsStr = (seconds % 60).toString().padLeft(2, '0');

    return "$minutesStr:$secondsStr";
  }
}
