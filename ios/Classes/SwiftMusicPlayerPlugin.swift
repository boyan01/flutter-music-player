import AVFoundation
import CoreAudio
import Flutter
import UIKit

public class SwiftMusicPlayerUiPlugin: NSObject, FlutterPlugin {
  let channel: FlutterMethodChannel

  let playerCallback: MusicPlayerCallback

  private let registrar: FlutterPluginRegistrar

  public init(channel: FlutterMethodChannel, registrar: FlutterPluginRegistrar) {
    self.channel = channel
    playerCallback = ChannelPlayerCallback(channel)
    self.registrar = registrar
    super.init()
    player.addCallback(playerCallback)
    player.playerSource = self
  }

  let player: MusicPlayer = MusicPlayer.shared

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
      result(player.metadata != nil)
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
      player.seekTo(Double(call.arguments as! Int) / 1000.0)
      result(nil)
      break
    case "playFromMediaId":
      player.playFromMediaId(call.arguments as! String)
      result(nil)
      break
    case "prepareFromMediaId":
      player.prepareFromMediaId(call.arguments as! String)
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
    methodChannel = channel
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

extension SwiftMusicPlayerUiPlugin: MusicPlayerSource {
  func getPlayUrl(mediaId: String, fallback: String?, completion: @escaping (String?) -> Void) {
    channel.invokeMethod("getPlayUrl", arguments: ["id": mediaId, "url": fallback]) { any in
      if let result = any as? String {
        completion(result)
      } else if FlutterMethodNotImplemented.isEqual(any) {
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

  func onNextNoMoreMusic(_ queue: PlayQueue, _ mode: PlayMode, completion: @escaping (MusicMetadata?) -> Void) {
    channel.invokeMethod("onPlayNextNoMoreMusic", arguments: [
      "queue": queue.toMap(),
      "playMode": mode.rawValue,
    ]) { result in
      if FlutterMethodNotImplemented.isEqual(result) {
        if mode == .shuffle {
          queue.generateShuffleList()
        }
        completion(queue.getNext(nil, playMode: mode))
      } else {
        completion(MusicMetadata(any: result))
      }
    }
  }

  func onPreviousNoMoreMusic(_ queue: PlayQueue, _ mode: PlayMode, completion: @escaping (MusicMetadata?) -> Void) {
    channel.invokeMethod("onPlayPreviousNoMoreMusic", arguments: [
      "queue": queue.toMap(),
      "playMode": mode.rawValue,
    ]) { result in
      if FlutterMethodNotImplemented.isEqual(result) {
        if mode == .shuffle {
          queue.generateShuffleList()
        }
        completion(queue.getPrevious(nil, playMode: mode))
      } else {
        completion(MusicMetadata(any: result))
      }
    }
  }

  func loadAssetResource(url: URL) -> String {
    let assetKey = registrar.lookupKey(forAsset: url.path)
    guard let path = Bundle.main.path(forResource: assetKey, ofType: nil) else {
      debugPrint("resource not found : \(assetKey)")
      return url.absoluteString
    }
    debugPrint("resource found: \(path)")
    return URL(fileURLWithPath: path).absoluteString
  }
}
