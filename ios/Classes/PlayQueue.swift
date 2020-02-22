//
// Created by yangbin on 2020/2/22.
//

import Foundation

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
