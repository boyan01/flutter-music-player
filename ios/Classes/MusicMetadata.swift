//
// Created by yangbin on 2020/2/22.
//

import Foundation
import SwiftAudio

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


class MetadataAudioItem: AudioItem, RemoteCommandable {
    func getCommands() -> [RemoteCommand] {
        [.pause, .play, .stop, .next, .previous]
    }

    private let metadata: MusicMetadata

    private let uri: String

    private let servicePlugin: MusicPlayerServicePlugin

    public init(metadata: MusicMetadata, uri: String, servicePlugin: MusicPlayerServicePlugin) {
        self.metadata = metadata
        self.uri = uri
        self.servicePlugin = servicePlugin
    }

    func getSourceUrl() -> String {
        guard let url = URL(string: uri) else {
            return uri
        }
        if "asset".caseInsensitiveCompare(url.scheme ?? "") == .orderedSame {
            let assetKey = servicePlugin.registrar.lookupKey(forAsset: url.path)
            guard let path = Bundle.main.path(forResource: assetKey, ofType: nil) else {
                debugPrint("resource not found : \(assetKey)")
                return uri
            }
            return URL(fileURLWithPath: path).absoluteString
        }
        return uri
    }

    func getArtist() -> String? {
        metadata.subtitle
    }

    func getTitle() -> String? {
        metadata.title
    }

    func getAlbumTitle() -> String? {
        metadata.subtitle
    }

    func getSourceType() -> SourceType {
        .stream
    }

    func getArtwork(_ handler: @escaping (UIImage?) -> ()) {
        servicePlugin.loadImage(metadata: metadata, completion: handler)
    }
}