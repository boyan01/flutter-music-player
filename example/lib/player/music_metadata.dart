import 'package:music_player_example/player/player.dart';

/// Music metadata
class MusicMetadata extends PlayItem {
  final Map? extras;
  final String mediaId;
  final String? mediaUri;
  final String? title;
  final String? subtitle;

  final int duration;
  final String? iconUri;

  MusicMetadata({
    this.extras,
    required this.mediaId,
    this.title,
    this.subtitle,
    this.duration = 0,
    this.iconUri,
    this.mediaUri,
  });

  factory MusicMetadata.fromMap(Map map) {
    return MusicMetadata(
      extras: map["extras"],
      mediaId: map["mediaId"],
      title: map["title"],
      subtitle: map["subtitle"],
      duration: map["duration"],
      iconUri: map["iconUri"],
      mediaUri: map['mediaUri'],
    );
  }

  Map<String, dynamic> toMap() {
    return {
      "extras": extras,
      "mediaId": mediaId,
      "title": title,
      "subtitle": subtitle,
      "duration": duration,
      "iconUri": iconUri,
      "mediaUri": mediaUri,
    };
  }

  @override
  String toString() {
    return 'MusicMetadata{mediaId: $mediaId, title: $title, subtitle: $subtitle}';
  }

  @override
  Future<String> playerUrl() {
    return Future.value(mediaUri!);
  }
}
