import Flutter
import UIKit
import CoreAudio
import AVFoundation

public class SwiftMusicPlayerUiPlugin: NSObject, FlutterPlugin {

    let channel: FlutterMethodChannel

    let playerCallback: MusicPlayerCallback

    public init(channel: FlutterMethodChannel, registrar: FlutterPluginRegistrar) {
        self.channel = channel
        self.playerCallback = ChannelPlayerCallback(channel)
        self.player = MusicPlayer(registrar: registrar)
        super.init()
        player.addCallback(playerCallback)
    }

    let player: MusicPlayer

    static let UI_CHANNEL_NAME = "tech.soit.quiet/player.ui"

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: UI_CHANNEL_NAME, binaryMessenger: registrar.messenger())
        let instance = SwiftMusicPlayerUiPlugin(channel: channel, registrar: registrar)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "init":
            playerCallback.onPlayQueueChanged(player.playQueue)
            playerCallback.onPlaybackStateChanged(player.playbackState)
            playerCallback.onPlayModeChanged(player.playMode)
            playerCallback.onMetadataChanged(player.metadata)
            result(nil)
            break
        case "play":
            player.play()
            result(nil)
            break
        case "pause":
            player.pause()
            result(nil)
            break
        case "playFromMediaId":
            player.playFromMediaId(call.arguments as! String)
            result(nil)
            break
        case "skipToNext":
            player.skipToNext()
            result(nil)
            break
        case "skipToPrevious":
            player.skipToPrevious()
            result(nil)
            break
        case "setPlayMode":
            player.playMode = PlayMode(rawValue: call.arguments as! Int) ?? PlayMode.sequence
            result(nil)
            break
        case "setPlayQueue":
            player.playQueue = PlayQueue(map: call.arguments as! [String: Any?])
            result(nil)
            break
        case "getNext":
            result(player.getNext(anchor: MusicMetadata(any: call.arguments))?.toMap())
            break
        case "getPrevious":
            result(player.getPrevious(anchor: MusicMetadata(any: call.arguments))?.toMap())
            break
        case "insertToNext":
            player.addMetadata(MusicMetadata(any: call.arguments)!, anchorMediaId: player.metadata?.mediaId)
            result(nil)
            break
        default:
            result(FlutterMethodNotImplemented)
        }
    }

}


private class ChannelPlayerCallback: MusicPlayerCallback {

    private let methodChannel: FlutterMethodChannel

    init(_ channel: FlutterMethodChannel) {
        self.methodChannel = channel
    }

    func onPlaybackStateChanged(_ state: PlaybackState) {
        methodChannel.invokeMethod("onPlaybackStateChanged", arguments: state.toMap())
    }

    func onPlayQueueChanged(_ queue: PlayQueue) {
        methodChannel.invokeMethod("onPlayQueueChanged", arguments: queue.toMap())
    }

    func onMetadataChanged(_ metadata: MusicMetadata?) {
        methodChannel.invokeMethod("onMetadataChanged", arguments: metadata?.toMap())
    }

    func onPlayModeChanged(_ playMode: PlayMode) {
        methodChannel.invokeMethod("onPlayModeChanged", arguments: playMode.rawValue)
    }

}

public class MusicPlayerServicePlugin: NSObject, FlutterPlugin {


    public static func register(with registrar: FlutterPluginRegistrar) {
        fatalError("do not call register!")
    }

    public static func start() -> MusicPlayerServicePlugin {
        let engine = FlutterEngine(name: "player-service-engine")
        if (!engine.run(withEntrypoint: "playerBackgroundService")) {
            debugPrint("run 'playerBackgroundService' failed.")
        }
        let registrar = engine.registrar(forPlugin: String(describing: type(of: MusicPlayerServicePlugin.self)))
        let channel = FlutterMethodChannel(name: "tech.soit.quiet/background_callback", binaryMessenger: registrar.messenger())
        let plugin = MusicPlayerServicePlugin(channel)
        registrar.addMethodCallDelegate(plugin, channel: channel)
        return plugin
    }

    private let channel: FlutterMethodChannel

    private init(_ channel: FlutterMethodChannel) {
        self.channel = channel
        super.init()
    }

    public func handle(_ call: FlutterMethodCall, result: FlutterResult) {

    }

    func getPlayUrl(mediaId: String, fallback: String?, completion: @escaping (String?) -> Void) {
        channel.invokeMethod("getPlayUrl", arguments: ["id": mediaId, "url": fallback]) { any in
            if let result = any as? String {
                completion(result)
            } else if (FlutterMethodNotImplemented.isEqual(any)) {
                completion(fallback)
            } else {
                completion(nil)
            }
        }
    }

    func loadImage(metadata: MusicMetadata, completion: @escaping (UIImage?) -> Void) {
        channel.invokeMethod("loadImage", arguments: metadata.toMap()) { result in
            if let result = result as? FlutterStandardTypedData {
                completion(UIImage(data: result.data))
            } else {
                completion(nil)
            }
        }
    }

}