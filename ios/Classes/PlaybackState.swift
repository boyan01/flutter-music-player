//
// Created by yangbin on 2020/2/22.
//

import Foundation

struct PlaybackState {
    let state: State
    let position: TimeInterval
    let bufferedPosition: TimeInterval
    let speed: Float
    let error: PlaybackError?
    // duration in mills.
    let updateTime: Int
}

extension PlaybackState {
    func toMap() -> [String: Any?] {
        [
            "state": state.rawValue,
            "position": Int(position * 1000),
            "bufferedPosition": Int(bufferedPosition * 1000),
            "speed": speed,
            "error": error?.toMap(),
            "updateTime": updateTime,
        ]
    }
}

enum State: Int {
    case none = 0, paused, playing, buffering, error
}

enum ErrorType: Int {
    // detail see lib/src/playback_error.dart ErrorType.*
    case source = 0, render, unknown
}

struct PlaybackError {
    let type: ErrorType
    let message: String
}

extension PlaybackError {
    func toMap() -> [String: Any?] {
        [
            "type": type.rawValue,
            "message": message,
        ]
    }
}
