# flutter_music_player  [![](https://github.com/boyan01/flutter-music-player/workflows/Test%20and%20Build%20Apk/badge.svg)](https://github.com/boyan01/flutter-music-player/actions)

Media session framework plugin for flutter, make it easy to implement music play by flutter.

* [x] ios support
* [x] media style notification.
* [x] basic media control.
* [x] tracking player status change.

## Getting Started

1. Simple Usecase.

```dart
// Create Player instance.
MusicPlayer player = MusicPlayer()
 
// audio list
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

PlayQueue queue = PlayQueue(queueTitle: "Simple Test", queueId: "test1", queue: medias)

// Perform play operation.
player.playWithQueue(queue, metadata: medias.first)

```

2. Background control.

add a method named `playerBackgroundService` to your lib/maim.dart. 

* `playerBackgroundService` is background service FlutterEngin entry point.

```dart
@pragma("vm:entry-point")
void playerBackgroundService() {
  runBackgroundService(
    playUriInterceptor: (mediaId, fallbackUrl) async {
      debugPrint("get media play uri : $mediaId , $fallbackUrl");
      if (mediaId == 'rise') return "asset:///tracks/rise.mp3";
      return fallbackUrl;
    },
    imageLoadInterceptor: (metadata) async {
      debugPrint("load image for ${metadata.mediaId} , ${metadata.title}");
      if (metadata.mediaId == "bamboo") {
        final data = await rootBundle.load("images/bamboo.jpg");
        return Uint8List.view(data.buffer);
      }
      return null;
    },
    playQueueInterceptor: ExamplePlayQueueInterceptor(),
  );
}
```



# Thanks

thanks [jorgenhenrichsen/SwiftAudio](https://github.com/jorgenhenrichsen/SwiftAudio) for iOS support.

thanks [JetBrain](https://www.jetbrains.com/?from=flutter-netease-music) provide open source lincese AppCode for iOS programming.