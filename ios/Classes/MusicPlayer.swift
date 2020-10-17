//
// Created by BoYan on 2020/2/17.
//

import AVFoundation
import Foundation
import MediaPlayer
import SwiftAudio

///
/// Music Player.
///
class MusicPlayer: NSObject, MusicPlayerSession {
    private let shimPlayerCallback = ShimMusicPlayCallback()

    public static let shared = MusicPlayer()

    override private init() {
        super.init()
        player.event.stateChange.addListener(self) { _ in
            self.invalidatePlaybackState()
        }
        player.event.playbackEnd.addListener(self) { reason in
            if reason == .playedUntilEnd {
                self.skipToNext()
            }
        }
        player.event.updateDuration.addListener(self) { _ in
            self.invalidateMetadata()
        }
        player.event.seek.addListener(self) { _, finish in
            if finish {
                self.invalidatePlaybackState()
            }
        }
        player.event.fail.addListener(self) { data in
            var type: ErrorType
            switch self.player.playerState {
            case .playing:
                type = .render
                break
            case .buffering, .loading:
                type = .source
                break
            default:
                type = .unknown
                break
            }
            self.playbackError = PlaybackError(
                type: type,
                message: data?.localizedDescription ?? data.debugDescription
            )
            self.invalidatePlaybackState()
        }
        player.remoteCommandController.handleNextTrackCommand = handleNextTrackCommand
        player.remoteCommandController.handlePreviousTrackCommand = handlePreviousTrackCommand
    }

    private func handleNextTrackCommand(event: MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus {
        skipToNext()
        return MPRemoteCommandHandlerStatus.success
    }

    private func handlePreviousTrackCommand(event: MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus {
        skipToPrevious()
        return MPRemoteCommandHandlerStatus.success
    }

    private let player: AudioPlayer = AudioPlayer()

    /// fetching play uri from background service
    private var isPlayUriPrefetching = false

    private var playbackError: PlaybackError?

    private let servicePlugin = MusicPlayerServicePlugin.shared

    var playMode: PlayMode = .sequence {
        didSet {
            shimPlayerCallback.onPlayModeChanged(playMode)
        }
    }

    var metadata: MusicMetadata? {
        didSet {
            invalidateMetadata()
        }
    }

    var playQueue: PlayQueue = PlayQueue.Empty {
        willSet {
            playQueue.setChangeListener(nil)
        }
        didSet {
            playQueue.setChangeListener(invalidatePlayQueue)
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
        if isPlayUriPrefetching {
            state = .buffering
        }
        if playbackError != nil {
            state = .error
        }

        return PlaybackState(
            state: state,
            position: player.currentTime,
            bufferedPosition: player.bufferedPosition,
            speed: playbackRate,
            error: playbackError,
            updateTime: systemUptime())
    }

    private func performPlay(metadata: MusicMetadata?) {
        self.metadata = metadata
        player.stop()
        // clear error, prepare to play next item.
        playbackError = nil
        getPlayItemForPlay(metadata: metadata) { item in
            if let item = item {
                do {
                    do {
                        try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
                        try AVAudioSession.sharedInstance().setActive(true)
                    } catch {
                        debugPrint("active session errored: \(error)")
                    }
                    debugPrint("start play: \(String(describing: item.getTitle())) \(item.getSourceUrl())")
                    try self.player.load(item: item, playWhenReady: true)
                } catch {
                    self.playbackError = PlaybackError(
                        type: .source,
                        message: "create player failed : \(error)"
                    )
                }
            } else {
                self.playbackError = PlaybackError(
                    type: .source,
                    message: "error : can not get audio item for \(String(describing: metadata))"
                )
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
        do {
            try AVAudioSession.sharedInstance().setActive(false)
        } catch {
        }
    }

    var playbackRate: Float {
        set {
            player.rate = newValue
            invalidatePlaybackState()
        }
        get {
            player.rate
        }
    }

    func seekTo(_ pos: TimeInterval) {
        player.seek(to: pos)
    }

    func getNext(anchor: MusicMetadata?, completion: @escaping (MusicMetadata?) -> Void) {
        let metadata = playQueue.getNext(anchor, playMode: playMode)
        if metadata != nil {
            completion(metadata)
        } else {
            servicePlugin.onNextNoMoreMusic(playQueue, playMode, completion: completion)
        }
    }

    func getPrevious(anchor: MusicMetadata?, completion: @escaping (MusicMetadata?) -> Void) {
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
