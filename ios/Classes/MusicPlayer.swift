//
// Created by BoYan on 2020/2/17.
//

import Foundation
import AVFoundation

class MusicMetadata: Equatable {

    private let data: [String: Any]

    public init(map: [String: Any]) {
        self.data = map
    }

    convenience init?(any: Any?) {
        if let map = any as? [String: Any] {
            self.init(map: map)
        } else {
            return nil
        }
    }

    // mediaId of this metadata
    var mediaId: String {
        data["mediaId"] as! String
    }

    var subtitle: String? {
        data["subtitle"] as? String
    }

    var title: String? {
        data["title"] as? String
    }

    var mediaUri: String? {
        data["mediaUri"] as? String
    }

    static func ==(lhs: MusicMetadata, rhs: MusicMetadata) -> Bool {
        lhs.mediaId == rhs.mediaId
    }

    func copyNewDuration(duration: Int) -> MusicMetadata {
        var data = self.data
        data.updateValue(duration, forKey: "duration")
        return MusicMetadata(map: data)
    }

    func toMap() -> [String: Any] {
        data
    }

}

enum PlayMode: Int {
    case shuffle = 0
    case single
    case sequence
}


class PlayQueue {

    static let Empty = PlayQueue(queueId: "", queueTitle: "", queue: [], shuffleIds: [], extras: nil)

    private var queue: [MusicMetadata] = []
    private var shuffleIds: [String] = []
    private let extras: Any?
    private let queueId: String
    private let queueTitle: String?

    init(queueId: String, queueTitle: String?, queue: [MusicMetadata], shuffleIds: [String]?, extras: Any?) {
        self.queueId = queueId
        self.queueTitle = queueTitle
        self.queue.append(contentsOf: queue)
        let shuffled = shuffleIds ?? queue.shuffled().map { metadata -> String in
            metadata.mediaId
        }
        self.shuffleIds.append(contentsOf: shuffled)
        self.extras = extras
    }

    convenience init(map: [String: Any?]) {
        self.init(queueId: map["queueId"] as! String,
                queueTitle: map["queueTitle"] as? String,
                queue: (map["queue"] as! [[String: Any]]).map { dictionary -> MusicMetadata in
                    MusicMetadata(map: dictionary)
                },
                shuffleIds: map["shuffleQueue"] as? [String],
                extras: map["extras"] ?? nil)
    }

    func toMap() -> [String: Any?] {
        [
            "queueId": queueId,
            "queueTitle": queueTitle,
            "queue": queue.map { metadata -> [String: Any] in
                metadata.toMap()
            },
            "shuffleQueue": shuffleIds,
            "extras": extras
        ]
    }

    func add(_ music: MusicMetadata, anchor: String?) {
        if let anchor = anchor {
            let index = queue.firstIndex { metadata in
                metadata.mediaId == anchor
            } ?? (queue.count - 1)
            let shuffleIndex = shuffleIds.firstIndex(of: anchor) ?? (shuffleIds.count - 1)
            queue.insert(music, at: index + 1)
            shuffleIds.insert(music.mediaId, at: shuffleIndex + 1)
        } else {
            queue.append(music)
            shuffleIds.append(music.mediaId)
        }
    }

    func remove(mediaId: String) {
        queue.removeAll { metadata in
            metadata.mediaId == mediaId
        }
        shuffleIds.removeAll { s in
            s == mediaId
        }
    }

    func getNext(_ current: MusicMetadata?, playMode: PlayMode) -> MusicMetadata? {
        getMusicInternal(current, playMode, true)
    }

    func getPrevious(_ current: MusicMetadata?, playMode: PlayMode) -> MusicMetadata? {
        getMusicInternal(current, playMode, false)
    }

    private func getMusicInternal(_ current: MusicMetadata?, _ playMode: PlayMode, _ next: Bool) -> MusicMetadata? {
        if queue.isEmpty {
            debugPrint("empty play queue")
            return nil
        }
        if let anchor = current {
            switch playMode {
            case .single, .sequence:
                var index = queue.firstIndex { metadata in
                    metadata.mediaId == anchor.mediaId
                } ?? -1
                index = index + (next ? 1 : -1)
                if (index >= queue.count && next) {
                    return queue.first
                } else if (index <= -1 && !next) {
                    return queue.last
                } else {
                    return queue[index]
                }
            case .shuffle:
                var index = shuffleIds.firstIndex(of: anchor.mediaId) ?? -1;
                index = index + (next ? 1 : -1)
                if (index >= queue.count || index < 0) {
                    return nil
                } else {
                    return requireMusicItem(shuffleIds[index])
                }
            }
        } else if playMode == .shuffle {
            return requireMusicItem(shuffleIds[0])
        } else {
            return queue[0]
        }
    }


    private func requireMusicItem(_ mediaId: String) -> MusicMetadata? {
        queue.first { metadata in
            metadata.mediaId == mediaId
        }
    }

    func getByMediaId(_ mediaId: String) -> MusicMetadata? {
        requireMusicItem(mediaId)
    }
}


class MusicPlayer: NSObject, AVAudioPlayerDelegate, MusicPlayerSession {

    private let shimPlayerCallback = ShimMusicPlayCallback()

    private let registrar: FlutterPluginRegistrar

    public init(registrar: FlutterPluginRegistrar) {
        self.registrar = registrar
        super.init()
    }

    private var player: AVAudioPlayer? = nil

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
        if let player = self.player {
            return PlaybackState(state: player.state, position: player.currentTime,
                    bufferedPosition: 0, speed: 1.0, error: nil, updateTime: Date().timeIntervalSince1970)
        } else {
            return PlaybackState(state: .none, position: 0, bufferedPosition: 0, speed: 1,
                    error: nil, updateTime: Date().timeIntervalSince1970)
        }
    }


    private func performPlay(metadata: MusicMetadata?) {
        self.metadata = metadata
        player?.stop()
        player = nil
        if let url = getUrlForPlay(metadata: metadata) {
            debugPrint("performPlay : \(url)")
            do {
                let player = try AVAudioPlayer(contentsOf: url)
                player.play()
                player.delegate = self
                self.player = player
            } catch {
                debugPrint("create player failed : \(error) ")
            }
        }
    }

    private func getUrlForPlay(metadata: MusicMetadata?) -> URL? {
        guard let metadata = metadata else {
            return nil
        }
        guard let mediaUri = metadata.mediaUri else {
            return nil
        }
        guard let url = URL(string: mediaUri) else {
            return nil
        }
        if "asset".caseInsensitiveCompare(url.scheme ?? "") == .orderedSame {
            let assetKey = registrar.lookupKey(forAsset: url.path)
            guard let path = Bundle.main.path(forResource: assetKey, ofType: nil) else {
                debugPrint("resource not found : \(assetKey)")
                return nil
            }
            return URL(fileURLWithPath: path)
        } else {
            return url
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
        player?.stop()
        let skip = call()
        performPlay(metadata: skip)
    }


    func play() {
        player?.play()
    }

    func pause() {
        player?.pause()
    }

    func stop() {
        player?.stop()
    }

    func seekTo(_ pos: Int) {
        player?.play(atTime: Double(pos))
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

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        debugPrint("audioPlayerDidFinishPlaying")
        invalidatePlaybackState()
        skipToNext()
    }

    func audioPlayerBeginInterruption(_ player: AVAudioPlayer) {
        debugPrint("audioPlayerBeginInterruption")
        invalidatePlaybackState()
    }

    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        debugPrint("audioPlayerDecodeErrorDidOccur")
        invalidatePlaybackState()
    }

    func audioPlayerEndInterruption(_ player: AVAudioPlayer, withOptions flags: Int) {
        debugPrint("audioPlayerEndInterruption")
        invalidatePlaybackState()
    }

    private func invalidatePlayQueue() {
        shimPlayerCallback.onPlayQueueChanged(playQueue)
    }

    private func invalidateMetadata() {
        shimPlayerCallback.onMetadataChanged(metadata)
    }

    private func invalidatePlaybackState() {
        shimPlayerCallback.onPlaybackStateChanged(playbackState)
    }


}


struct PlaybackState {
    let state: State
    let position: TimeInterval
    let bufferedPosition: TimeInterval
    let speed: Float
    let error: Error?
    let updateTime: TimeInterval
}

extension PlaybackState {
    func toMap() -> [String: Any?] {
        [
            "state": state.rawValue,
            "position": Int(position * 1000),
            "bufferedPosition": Int(bufferedPosition * 1000),
            "speed": speed,
            "error": nil,
            "updateTime": Int(updateTime * 1000)
        ]
    }

}

extension AVAudioPlayer {

    var state: State {
        isPlaying ? .playing : .paused
    }

}

enum State: Int {
    case none = 0, paused, playing, buffering, error
}

