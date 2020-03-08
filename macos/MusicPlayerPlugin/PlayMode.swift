//
// Created by yangbin on 2020/2/22.
//

import Foundation

enum PlayMode: Equatable {
    case shuffle
    case single
    case sequence
    case undefined(value: Int)
}

extension PlayMode {

    static func from(rawValue: Int) -> PlayMode {
        switch (rawValue) {
        case 0:
            return PlayMode.shuffle
        case 1:
            return PlayMode.single
        case 2:
            return PlayMode.sequence
        default:
            return PlayMode.undefined(value: rawValue)
        }
    }

    var rawValue: Int {
        switch (self) {
        case .shuffle:
            return 0;
        case .single:
            return 1;
        case .sequence:
            return 2;
        case .undefined(let value):
            return value
        }
    }

}