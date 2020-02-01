import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:music_player/src/channel_ui.dart';
import 'package:music_player/src/player/music_metadata.dart';
import 'package:music_player/src/player/music_player_callback.dart';
import 'package:music_player/src/player/play_queue.dart';
import 'package:music_player/src/player/playback_state.dart';

import 'ext_copy.dart';
import 'serialization.dart';

mixin ChannelPlayerCallbackAdapter on ValueNotifier<MusicPlayerValue> implements MusicPlayerCallback {
  bool handleRemoteCall(MethodCall call) {
    switch (call.method) {
      case 'onPlaybackStateChanged':
        break;
      case 'onMetadataChanged':
        onMetadataChange(createMusicMetadata(call.arguments));
        break;
      case 'onPlayQueueChanged':
        onPlayQueueChanged(createPlayQueue(call.arguments));
        break;
      case 'onPlayModeChanged':
        onPlayModeChanged(call.arguments);
        break;
      default:
        return false;
    }
    return true;
  }

  @override
  void onMetadataChange(MusicMetadata metadata) {
    value = value.copy(metadata: metadata);
  }

  @override
  void onPlayModeChanged(int playMode) {
    value = value.copy(playMode: playMode);
  }

  @override
  void onPlayQueueChanged(PlayQueue queue) {
    value = value.copy(queue: queue);
  }

  @override
  void onPlaybackStateChanged(PlaybackState state) {
    value = value.copy(state: state);
  }
}
