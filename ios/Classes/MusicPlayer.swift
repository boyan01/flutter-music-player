//
// Created by BoYan on 2020/2/17.
//

import Foundation
import AVFoundation
import SwiftAudio


class MusicPlayer: NSObject, MusicPlayerSession {

    private let shimPlayerCallback = ShimMusicPlayCallback()

    private let registrar: FlutterPluginRegistrar

    public init(registrar: FlutterPluginRegistrar) {
        self.registrar = registrar
        self.player = AudioPlayer()
        super.init()
        player.event.stateChange.addListener(self) { state in
            self.invalidatePlaybackState()
        }
        player.event.playbackEnd.addListener(self) { reason in
            if reason == .playedUntilEnd {
                self.skipToNext()
            }
        }
        player.event.updateDuration.addListener(self) { v in
            self.invalidateMetadata()
        }
    }

    private let player: AudioPlayer

    /// fetching play uri from background service
    private var isPlayUriPrefetching = false

    private let servicePlugin = MusicPlayerServicePlugin.start()

    var playMode: PlayMode = .sequence {
        didSet {
            shimPlayerCallback.onPlayModeChanged(playMode)
        }
    }

    var metadata: MusicMetadata? = nil {
        didSet {
            invalidateMetadata()
        }
    }

    var playQueue: PlayQueue = PlayQueue.Empty {
        didSet {
            invalidatePlayQueue()
        }
    }

    var playbackState: PlaybackState {
        var state: State
        switch player.playerState {
        case .idle:
            state = .none
            break
        case .paused, .ready:
            state = .paused
            break
        case .playing:
            state = .playing
            break
        case .buffering, .loading:
            state = .buffering
            break
        }
        if (isPlayUriPrefetching) {
            state = .buffering
        }
        return PlaybackState(
                state: state,
                position: player.currentTime,
                bufferedPosition: player.bufferedPosition,
                speed: player.rate,
                error: nil,
                updateTime: Date().timeIntervalSince1970)
    }


    private func performPlay(metadata: MusicMetadata?) {
        self.metadata = metadata
        player.stop()
        getPlayItemForPlay(metadata: metadata) { item in
            if let item = item {
                do {
                    try self.player.load(item: item, playWhenReady: true)
                } catch {
                    debugPrint("create player failed : \(error) ")
                }
            } else {
                // TODO handle error
            }
            self.isPlayUriPrefetching = false
            self.invalidatePlaybackState()
        }
        isPlayUriPrefetching = true
        invalidatePlaybackState()
    }

    private func getPlayItemForPlay(metadata: MusicMetadata?, completion: @escaping (AudioItem?) -> Void) {
        guard let metadata = metadata else {
            completion(nil)
            return
        }
        servicePlugin.getPlayUrl(mediaId: metadata.mediaId, fallback: metadata.mediaUri) { url in
            if let url = url {
                completion(MetadataAudioItem(metadata: metadata, uri: url,
                        registrar: self.registrar, servicePlugin: self.servicePlugin))
            } else {
                completion(nil)
            }
        }
    }

    func skipToNext() {
        skipTo {
            getNext(anchor: metadata)
        }
    }

    func skipToPrevious() {
        skipTo {
            getPrevious(anchor: metadata)
        }
    }

    func playFromMediaId(_ mediaId: String) {
        skipTo {
            playQueue.getByMediaId(mediaId)
        }
    }

    private func skipTo(execute call: () -> MusicMetadata?) {
        player.stop()
        let skip = call()
        performPlay(metadata: skip)
    }


    func play() {
        player.play()
    }

    func pause() {
        player.pause()
    }

    func stop() {
        player.stop()
    }

    func seekTo(_ pos: TimeInterval) {
        player.seek(to: pos)
    }

    func getNext(anchor: MusicMetadata?) -> MusicMetadata? {
        playQueue.getNext(metadata, playMode: playMode)
    }

    func getPrevious(anchor: MusicMetadata?) -> MusicMetadata? {
        playQueue.getPrevious(anchor, playMode: playMode)
    }

    func addMetadata(_ metadata: MusicMetadata, anchorMediaId: String?) {
        playQueue.add(metadata, anchor: anchorMediaId)
        invalidatePlayQueue()
    }

    func removeMetadata(mediaId: String) {
        playQueue.remove(mediaId: mediaId)
        invalidatePlayQueue()
    }

    func addCallback(_ callback: MusicPlayerCallback) {
        shimPlayerCallback.addCallback(callback)
    }

    func removeCallback(_ callback: MusicPlayerCallback) {
        shimPlayerCallback.removeCallback(callback)
    }

    private func invalidatePlayQueue() {
        shimPlayerCallback.onPlayQueueChanged(playQueue)
    }

    private func invalidateMetadata() {
        if player.duration > 0 {
            shimPlayerCallback.onMetadataChanged(metadata?.copyNewDuration(duration: Int(player.duration * 1000)))
        } else {
            shimPlayerCallback.onMetadataChanged(metadata)
        }
    }

    private func invalidatePlaybackState() {
        shimPlayerCallback.onPlaybackStateChanged(playbackState)
    }


}

