//
// Created by yangbin on 2020/8/22.
//

import Flutter

extension FlutterMethodCall {

    func requireArgs<T>() -> T {
        arguments as! T
    }

}