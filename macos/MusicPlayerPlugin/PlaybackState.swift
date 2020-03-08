//
// Created by yangbin on 2020/2/22.
//

import Foundation

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

enum State: Int {
    case none = 0, paused, playing, buffering, error
}

