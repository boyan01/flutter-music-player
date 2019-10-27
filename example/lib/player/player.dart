import 'package:flutter/material.dart';
import 'package:music_player/music_player.dart';

export 'package:music_player/music_player.dart';

final medias = [
  MediaMetadata(
    title: "Zhu Lin Jian",
    artist: "SanWu marblue",
    mediaId: "bamboo",
    mediaUri: "asset:///flutter_assets/tracks/bamboo.mp3",
    artUri: "https://via.placeholder.com/150/FFCA28/000000/?text=bamboo",
  ),
  MediaMetadata(
    title: "Rise",
    artist: "The Glitch Mob",
    mediaId: "rise",
    mediaUri: "asset:///flutter_assets/tracks/rise.mp3",
    artUri: "https://via.placeholder.com/150/4CAF50/FFFFFF/?text=Rise",
  ),
  MediaMetadata(
    title: "Cang",
    artist: "xu meng yuan",
    mediaId: "hide",
    mediaUri: "asset:///flutter_assets/tracks/hide.mp3",
    artUri: "https://via.placeholder.com/150/03A9F4/000000/?text=Cang",
  ),
];

class PlayerWidget extends StatefulWidget {
  final Widget child;

  const PlayerWidget({Key key, this.child}) : super(key: key);

  static TransportControls transportControls(BuildContext context) {
    return player(context).transportControls;
  }

  static MusicPlayer player(BuildContext context) {
    final _PlayerWidgetState state = context.ancestorStateOfType(const TypeMatcher<_PlayerWidgetState>());
    return state.player;
  }

  @override
  _PlayerWidgetState createState() => _PlayerWidgetState();
}

class _PlayerWidgetState extends State<PlayerWidget> {
  MusicPlayer player;

  @override
  void initState() {
    super.initState();
    player = MusicPlayer()
      ..addListener(() {
        setState(() {});
      });
  }

  @override
  void dispose() {
    super.dispose();
    player.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PlayerState(child: widget.child, state: player.value);
  }
}

class PlayerState extends InheritedWidget {
  final MusicPlayerValue state;

  const PlayerState({
    Key key,
    @required this.state,
    @required Widget child,
  })  : assert(child != null),
        super(key: key, child: child);

  static MusicPlayerValue of(BuildContext context) {
    final widget = context.inheritFromWidgetOfExactType(PlayerState) as PlayerState;
    return widget.state;
  }

  @override
  bool updateShouldNotify(PlayerState old) {
    return old.state != state;
  }
}
