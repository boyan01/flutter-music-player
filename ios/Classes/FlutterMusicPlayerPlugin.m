#import "FlutterMusicPlayerPlugin.h"
#import <flutter_music_player/flutter_music_player-Swift.h>

@implementation FlutterMusicPlayerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterMusicPlayerPlugin registerWithRegistrar:registrar];
}
@end
