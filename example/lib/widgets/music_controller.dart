import 'package:flutter/material.dart';
import 'package:music_player_example/player/player.dart';

class MusicControlBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bottomPadding = MediaQuery.of(context).viewPadding.bottom;
    final MusicPlayerState state = PlayerState.of(context);
    var description = state.metadata?.getDescription();
    if (state.playbackState.state == PlaybackState.STATE_NONE) {
      return Container();
    }
    return Container(
      padding: EdgeInsets.only(bottom: bottomPadding),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          _ControllerBar(),
          Text.rich(TextSpan(children: [
            TextSpan(text: 'current metadata:'),
            TextSpan(text: description?.title),
          ])),
          Text.rich(TextSpan(children: [
            TextSpan(text: state.queueTitle + "\n"),
            TextSpan(text: state.queue.join()),
          ])),
          Text.rich(TextSpan(children: [
            TextSpan(text: "playback state : \n"),
            TextSpan(text: """
speed:  ${state.playbackState.playbackSpeed} 
bufferedPosition:  ${state.playbackState.bufferedPosition} 
position:  ${state.playbackState.position} 
errorCode:  ${state.playbackState.errorCode} 
updateTime:  ${DateTime.fromMillisecondsSinceEpoch(state.playbackState.lastPositionUpdateTime).toIso8601String()} 
activeItemId:  ${state.playbackState.activeQueueItemId} 
          """),
          ])),
        ],
      ),
    );
  }
}

class _ControllerBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ButtonBar(
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
    );
  }
}

class PlayPauseButton extends StatelessWidget {
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
          tooltip: "play mode",
          onPressed: () {
            PlayerWidget.transportControls(context).play();
          });
    }
  }
}

class RepeatModelButton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final MusicPlayerState state = PlayerState.of(context);
    final int shuffleMode = state.shuffleMode;
    final int repeatMode = state.repeatMode;
    final _PlayMode playMode = _PlayMode(shuffleMode, repeatMode);

    Widget icon;
    if (playMode == shuffle) {
      icon = const Icon(Icons.shuffle);
    } else if (playMode == single) {
      icon = const Icon(Icons.repeat_one);
    } else {
      icon = const Icon(Icons.repeat);
      if (playMode != sequence) {
        _setPlayModel(context, sequence);
      }
    }
    return IconButton(
        icon: icon,
        onPressed: () {
          _setPlayModel(context, playMode.next());
        });
  }

  void _setPlayModel(BuildContext context, _PlayMode mode) {
    PlayerWidget.transportControls(context).setRepeatMode(mode.repeatMode);
    PlayerWidget.transportControls(context).setShuffleMode(mode.shuffleMode);
  }
}

const _PlayMode sequence = _PlayMode(PlaybackState.SHUFFLE_MODE_NONE, PlaybackState.REPEAT_MODE_ALL);
const _PlayMode single = _PlayMode(PlaybackState.SHUFFLE_MODE_NONE, PlaybackState.REPEAT_MODE_ONE);
const _PlayMode shuffle = _PlayMode(PlaybackState.SHUFFLE_MODE_ALL, PlaybackState.REPEAT_MODE_ALL);

class _PlayMode {
  final int shuffleMode;
  final int repeatMode;

  const _PlayMode(this.shuffleMode, this.repeatMode);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is _PlayMode &&
          runtimeType == other.runtimeType &&
          shuffleMode == other.shuffleMode &&
          repeatMode == other.repeatMode;

  @override
  int get hashCode => shuffleMode.hashCode ^ repeatMode.hashCode;

  _PlayMode next() {
    if (this == sequence) return single;
    if (this == single) return shuffle;
    return sequence;
  }
}
