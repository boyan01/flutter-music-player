//
// Created by yangbin on 2020/2/22.
//

import Foundation
import SwiftAudioEx

class MusicMetadata: Equatable {
  private let data: [String: Any]

  public init(map: [String: Any]) {
    data = map
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

  static func == (lhs: MusicMetadata, rhs: MusicMetadata) -> Bool {
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

  private let source: MusicPlayerSource

  public init(metadata: MusicMetadata, uri: String, source: MusicPlayerSource) {
    self.metadata = metadata
    self.uri = uri
    self.source = source
  }

  func getSourceUrl() -> String {
    guard let url = URL(string: uri) else {
      return uri
    }
    if "asset".caseInsensitiveCompare(url.scheme ?? "") == .orderedSame {
      return source.loadAssetResource(url: url)
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

  func getArtwork(_ handler: @escaping (UIImage?) -> Void) {
    source.loadImage(metadata: metadata, completion: handler)
  }
}
