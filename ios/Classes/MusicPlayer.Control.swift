//
// Created by yangbin on 2020/2/21.
//

import Foundation

protocol MusicPlayerSession: MusicPlayerCallbackContainer {

    var playMode: PlayMode { get set }

    var playQueue: PlayQueue { get set }

    var playbackState: PlaybackState { get }

    func play()

    func playFromMediaId(_ mediaId: String)

    func pause()

    func stop()

    func seekTo(_ pos: TimeInterval)

    func skipToNext()

    func skipToPrevious()

    func addMetadata(_ metadata: MusicMetadata, anchorMediaId: String?)

    func removeMetadata(mediaId: String)

    func getNext(anchor: MusicMetadata?, completion: @escaping (MusicMetadata?) -> Void)

    func getPrevious(anchor: MusicMetadata?, completion: @escaping (MusicMetadata?) -> Void)

}

protocol MusicPlayerCallbackContainer {

    func addCallback(_ callback: MusicPlayerCallback)

    func removeCallback(_ callback: MusicPlayerCallback)
}

protocol MusicPlayerCallback: AnyObject {

    func onPlaybackStateChanged(_ state: PlaybackState)

    func onPlayQueueChanged(_ queue: PlayQueue)

    func onMetadataChanged(_ metadata: MusicMetadata?)

    func onPlayModeChanged(_ playMode: PlayMode)

}


class ShimMusicPlayCallback: MusicPlayerCallback, MusicPlayerCallbackContainer {

    private var callbacks: [MusicPlayerCallback] = []

    func addCallback(_ callback: MusicPlayerCallback) {
        callbacks.append(callback)
    }

    func removeCallback(_ callback: MusicPlayerCallback) {
        callbacks.removeAll { item in
            item === callback
        }
    }

    func onPlaybackStateChanged(_ state: PlaybackState) {
        callbacks.forEach { callback in
            callback.onPlaybackStateChanged(state)
        }
    }

    func onPlayQueueChanged(_ queue: PlayQueue) {
        callbacks.forEach { callback in
            callback.onPlayQueueChanged(queue)
        }
    }

    func onMetadataChanged(_ metadata: MusicMetadata?) {
        callbacks.forEach { callback in
            callback.onMetadataChanged(metadata)
        }
    }

    func onPlayModeChanged(_ playMode: PlayMode) {
        callbacks.forEach { callback in
            callback.onPlayModeChanged(playMode)
        }
    }


}