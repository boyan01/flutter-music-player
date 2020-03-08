//
// Created by yangbin on 2020/2/22.
//

import Foundation

class PlayQueue {

    private typealias QueueChangeListener = () -> Void

    static let Empty = PlayQueue(queueId: "", queueTitle: "", queue: [], shuffleIds: [], extras: nil)

    private var queue: [MusicMetadata] = []
    private var shuffleIds: [String] = []
    private let extras: Any?
    private let queueId: String
    private let queueTitle: String?

    private var queueChangeListener: QueueChangeListener? = nil

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

    func setChangeListener(_ listener: (() -> Void)?) {
        self.queueChangeListener = listener
    }

    func generateShuffleList() {
        shuffleIds.removeAll()
        shuffleIds.append(contentsOf: queue.shuffled().map { metadata -> String in
            metadata.mediaId
        })
        if let listener = queueChangeListener {
            listener()
        }
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
                if (index >= queue.count || index <= -1) {
                    return nil
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
            case .undefined:
                return nil

            }
        } else {
            let index = next ? 0 : (queue.count - 1)
            return playMode == .shuffle ? requireMusicItem(shuffleIds[index]) : queue[index]
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

    func insert(_ index: Int, _ list: [MusicMetadata]) {
        queue.insert(contentsOf: list, at: index)
    }

}
