//
//  Clock.swift
//  music_player
//
//  Created by yangbin on 2020/10/14.
//

import Foundation

func systemUptime() -> Int {
    var spec = timespec()
    clock_gettime(CLOCK_UPTIME_RAW, &spec)
    return spec.tv_sec * 1000 + spec.tv_nsec / 1000000
}
