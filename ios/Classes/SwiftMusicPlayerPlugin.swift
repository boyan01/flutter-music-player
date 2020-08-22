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
        super.init()
        player.addCallback(playerCallback)
    }

    let player: MusicPlayer = MusicPlayer.shared

    static let UI_CHANNEL_NAME = "tech.soit.quiet/player.ui"

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: UI_CHANNEL_NAME, binaryMessenger: registrar.messenger())
        let instance = SwiftMusicPlayerUiPlugin(channel: channel, registrar: registrar)
        registrar.addMethodCallDelegate(instance, channel: channel)
        MusicPlayerServicePlugin.shared.start()
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
        case "seekTo":
            player.seekTo(Double((call.arguments as! Int)) / 1000.0)
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
            player.playMode = PlayMode.from(rawValue: call.arguments as! Int)
            result(nil)
            break
        case "setPlayQueue":
            player.playQueue = PlayQueue(map: call.arguments as! [String: Any?])
            result(nil)
            break
        case "getNext":
            player.getNext(anchor: MusicMetadata(any: call.arguments)) { metadata in
                result(metadata?.toMap())
            }
            break
        case "getPrevious":
            player.getPrevious(anchor: MusicMetadata(any: call.arguments)) { metadata in
                result(metadata?.toMap())
            }
            break
        case "insertToNext":
            player.addMetadata(MusicMetadata(any: call.arguments)!, anchorMediaId: player.metadata?.mediaId)
            result(nil)
            break
        case "setPlaybackSpeed":
            player.playbackRate = call.requireArgs()
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

    public static let shared = MusicPlayerServicePlugin()

    public static func register(with registrar: FlutterPluginRegistrar) {
        fatalError("do not call register!")
    }

    private let engine: FlutterEngine

    private var started: Bool = false

    private var channel: FlutterMethodChannel? = nil

    public var registrar: FlutterPluginRegistrar? = nil

    override private init() {
        engine = FlutterEngine(name: "player-service-engine")
        super.init()
    }

    func start() {
        if (started) {
            return
        }
        started = true;
        if (!engine.run(withEntrypoint: "playerBackgroundService", libraryURI: nil)) {
            debugPrint("run 'playerBackgroundService' failed.")
        }
        DispatchQueue.main.async(execute: waitEngineRunning)
    }

    private func waitEngineRunning() {
        if (engine.isolateId != nil) {
            debugPrint("service engine: \(engine.isolateId ?? "nil")")
            let registrar = engine.registrar(forPlugin: String(describing: type(of: MusicPlayerServicePlugin.self)))!
            let channel = FlutterMethodChannel(name: "tech.soit.quiet/background_callback", binaryMessenger: registrar.messenger())
            registrar.addMethodCallDelegate(self, channel: channel)
            // invoke GeneratedPluginRegistrant by selector.
            if let a = NSClassFromString("GeneratedPluginRegistrant") as? NSObject.Type {
                a.perform(NSSelectorFromString("registerWithRegistry:"), with: engine as FlutterPluginRegistry)
            } else {
                debugPrint("Tried to automatically register plugins with FlutterEngine \(engine) but could not find and invoke the GeneratedPluginRegistrant.")
            }
            self.registrar = registrar
            self.channel = channel
        } else {
            DispatchQueue.main.async(execute: waitEngineRunning)
        }
    }


    public func handle(_ call: FlutterMethodCall, result: FlutterResult) {
        switch call.method {
        case "insertToPlayQueue":
            let arg = call.arguments as! [String: Any]
            let list = (arg["list"] as! [[String: Any]]).map { map -> MusicMetadata in
                MusicMetadata(map: map)
            }
            let index = arg["index"] as! Int
            MusicPlayer.shared.insertMetadataList(list, index)
            result(nil)
            break
        case "updateConfig":
            //TODO config handle
            result(nil)
            break
        default:
            result(FlutterMethodNotImplemented)
        }
    }

    func getPlayUrl(mediaId: String, fallback: String?, completion: @escaping (String?) -> Void) {
        if let channel = self.channel {
            channel.invokeMethod("getPlayUrl", arguments: ["id": mediaId, "url": fallback]) { any in
                if let result = any as? String {
                    completion(result)
                } else if (FlutterMethodNotImplemented.isEqual(any)) {
                    completion(fallback)
                } else {
                    completion(nil)
                }
            }
        } else {
            completion(fallback)
        }
    }

    func loadImage(metadata: MusicMetadata, completion: @escaping (UIImage?) -> Void) {
        if let channel = self.channel {
            channel.invokeMethod("loadImage", arguments: metadata.toMap()) { result in
                if let result = result as? FlutterStandardTypedData {
                    completion(UIImage(data: result.data))
                } else {
                    completion(nil)
                }
            }
        } else {
            completion(nil)
        }
    }

    func onNextNoMoreMusic(_ queue: PlayQueue, _ mode: PlayMode, completion: @escaping (MusicMetadata?) -> ()) {
        if let channel = channel {
            channel.invokeMethod("onPlayNextNoMoreMusic", arguments: [
                "queue": queue.toMap(),
                "playMode": mode.rawValue
            ]) { result in
                if FlutterMethodNotImplemented.isEqual(result) {
                    if (mode == .shuffle) {
                        queue.generateShuffleList()
                    }
                    completion(queue.getNext(nil, playMode: mode))
                } else {
                    completion(MusicMetadata(any: result))
                }
            }
        } else {
            completion(queue.getNext(nil, playMode: mode))
        }
    }

    func onPreviousNoMoreMusic(_ queue: PlayQueue, _ mode: PlayMode, completion: @escaping (MusicMetadata?) -> ()) {
        if let channel = channel {
            channel.invokeMethod("onPlayPreviousNoMoreMusic", arguments: [
                "queue": queue.toMap(),
                "playMode": mode.rawValue
            ]) { result in
                if FlutterMethodNotImplemented.isEqual(result) {
                    if (mode == .shuffle) {
                        queue.generateShuffleList()
                    }
                    completion(queue.getPrevious(nil, playMode: mode))
                } else {
                    completion(MusicMetadata(any: result))
                }
            }
        } else {
            completion(queue.getPrevious(nil, playMode: mode))
        }
    }


}
