import 'dart:typed_data';

/// A simple set of metadata for a media item suitable for display. This can be
/// created using the Builder or retrieved from existing metadata using
/// [MediaMetadata.getDescription].
class MediaDescription {
  static const STATUS_NOT_DOWNLOADED = 0;
  static const STATUS_DOWNLOADING = 1;
  static const STATUS_DOWNLOADED = 2;

  final String mediaId;
  final String title;
  final String subtitle;
  final String description;
  final Uint8List iconBitmap;
  final String iconUri;
  final Map extras;

//<editor-fold desc="Data Methods" defaultstate="collapsed">

  const MediaDescription({
    this.mediaId,
    this.title,
    this.subtitle,
    this.description,
    this.iconBitmap,
    this.iconUri,
    this.extras,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is MediaDescription &&
          runtimeType == other.runtimeType &&
          mediaId == other.mediaId &&
          title == other.title &&
          subtitle == other.subtitle &&
          description == other.description &&
          iconBitmap == other.iconBitmap &&
          iconUri == other.iconUri &&
          extras == other.extras);

  @override
  int get hashCode =>
      mediaId.hashCode ^
      title.hashCode ^
      subtitle.hashCode ^
      description.hashCode ^
      iconBitmap.hashCode ^
      iconUri.hashCode ^
      extras.hashCode;

  @override
  String toString() {
    return 'MediaDescription{' +
        ' mediaId: $mediaId,' +
        ' title: $title,' +
        ' subtitle: $subtitle,' +
        ' description: $description,' +
        ' iconBitmap: $iconBitmap,' +
        ' iconUri: $iconUri,' +
        ' extras: $extras,' +
        '}';
  }

  MediaDescription copy({
    String mediaId,
    String title,
    String subtitle,
    String description,
    Uint8List iconBitmap,
    String iconUri,
    Map extras,
  }) {
    return MediaDescription(
      mediaId: mediaId ?? this.mediaId,
      title: title ?? this.title,
      subtitle: subtitle ?? this.subtitle,
      description: description ?? this.description,
      iconBitmap: iconBitmap ?? this.iconBitmap,
      iconUri: iconUri ?? this.iconUri,
      extras: extras ?? this.extras,
    );
  }

  Map<String, dynamic> toMap({
    String keyMapper(String key),
  }) {
    keyMapper ??= (key) => key;

    return {
      keyMapper('mediaId'): this.mediaId,
      keyMapper('title'): this.title,
      keyMapper('subtitle'): this.subtitle,
      keyMapper('description'): this.description,
      keyMapper('iconBitmap'): this.iconBitmap,
      keyMapper('iconUri'): this.iconUri,
      keyMapper('extras'): this.extras,
    };
  }

  factory MediaDescription.fromMap(
    Map<String, dynamic> map, {
    String keyMapper(String key),
  }) {
    keyMapper ??= (key) => key;

    return MediaDescription(
      mediaId: map[keyMapper('mediaId')] as String,
      title: map[keyMapper('title')] as String,
      subtitle: map[keyMapper('subtitle')] as String,
      description: map[keyMapper('description')] as String,
      iconBitmap: map[keyMapper('iconBitmap')] as Uint8List,
      iconUri: map[keyMapper('iconUri')] as String,
      extras: map[keyMapper('extras')] as Map,
    );
  }

//</editor-fold>
}
