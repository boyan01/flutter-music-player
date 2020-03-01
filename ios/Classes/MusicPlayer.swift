//
// Created by BoYan on 2020/2/17.
//

import Foundation
import AVFoundation
import SwiftAudio


class MusicPlayer: NSObject, MusicPlayerSession {

    private let shimPlayerCallback = ShimMusicPlayCallback()


    public static let shared = MusicPlayer()

    private override init() {
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

    private let player: AudioPlayer = AudioPlayer()

    /// fetching play uri from background service
    private var isPlayUriPrefetching = false

    private lazy var servicePlugin = MusicPlayerServicePlugin.start()

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
        willSet {
            self.playQueue.setChangeListener(nil)
        }
        didSet {
            self.playQueue.setChangeListener(invalidatePlayQueue)
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
                completion(MetadataAudioItem(metadata: metadata, uri: url, servicePlugin: self.servicePlugin))
            } else {
                completion(nil)
            }
        }
    }

    func skipToNext() {
        skipTo { performPlay in
            getNext(anchor: metadata, completion: performPlay)
        }
    }

    func skipToPrevious() {
        skipTo { performPlay in
            getPrevious(anchor: metadata, completion: performPlay)
        }
    }

    func playFromMediaId(_ mediaId: String) {
        skipTo { performPlay in
            performPlay(playQueue.getByMediaId(mediaId))
        }
    }

    private func skipTo(execute call: (_ completion: @escaping (MusicMetadata?) -> Void) -> Void) {
        player.stop()
        call { metadata in
            self.performPlay(metadata: metadata)
        }
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

    func getNext(anchor: MusicMetadata?, completion: @escaping (MusicMetadata?) -> ()) {
        let metadata = playQueue.getNext(anchor, playMode: playMode)
        if metadata != nil {
            completion(metadata)
        } else {
            servicePlugin.onNextNoMoreMusic(playQueue, playMode, completion: completion)
        }
    }

    func getPrevious(anchor: MusicMetadata?, completion: @escaping (MusicMetadata?) -> ()) {
        let metadata = playQueue.getPrevious(anchor, playMode: playMode)
        if metadata != nil {
            completion(metadata)
        } else {
            servicePlugin.onPreviousNoMoreMusic(playQueue, playMode, completion: completion)
        }
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

    func insertMetadataList(_ list: [MusicMetadata], _ index: Int) {
        playQueue.insert(index, list)
        invalidatePlayQueue()
    }

    private func invalidatePlaybackState() {
        shimPlayerCallback.onPlaybackStateChanged(playbackState)
    }


}

