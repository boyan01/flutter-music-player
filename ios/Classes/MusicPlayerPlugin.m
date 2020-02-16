#import "MusicPlayerPlugin.h"
#if __has_include(<music_player/music_player-Swift.h>)
#import <music_player/music_player-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "music_player-Swift.h"
#endif

@implementation MusicPlayerUiPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftMusicPlayerUiPlugin registerWithRegistrar:registrar];
}
@end
