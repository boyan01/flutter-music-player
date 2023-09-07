import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:music_player/music_player.dart';

class ProgressTrackingContainer extends StatefulWidget {
  final MusicPlayer player;
  final WidgetBuilder builder;

  const ProgressTrackingContainer({
    Key? key,
    required this.builder,
    required this.player,
  }) : super(key: key);

  @override
  State<ProgressTrackingContainer> createState() =>
      _ProgressTrackingContainerState();
}

class _ProgressTrackingContainerState extends State<ProgressTrackingContainer>
    with SingleTickerProviderStateMixin {
  late MusicPlayer _player;

  late Ticker _ticker;

  @override
  void initState() {
    super.initState();
    _player = widget.player;
    _player.stateNotifier.addListener(_onStateChanged);
    _player.playWhenReadyNotifier.addListener(_onStateChanged);
    _ticker = createTicker((elapsed) {
      setState(() {});
    });
    _onStateChanged();
  }

  void _onStateChanged() {
    final needTrack =
        _player.state == PlayerState.ready && _player.playWhenReady;
    if (_ticker.isActive == needTrack) return;
    if (_ticker.isActive) {
      _ticker.stop();
    } else {
      _ticker.start();
    }
  }

  @override
  void dispose() {
    _player.stateNotifier.removeListener(_onStateChanged);
    _player.playWhenReadyNotifier.removeListener(_onStateChanged);
    _ticker.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return widget.builder(context);
  }
}
