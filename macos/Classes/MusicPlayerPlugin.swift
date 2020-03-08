import Cocoa
import FlutterMacOS
import AVFoundation

public class MusicPlayerPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        SwiftMusicPlayerUiPlugin.register(with: registrar)
    }
}
