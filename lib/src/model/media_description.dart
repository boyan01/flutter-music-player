import 'dart:typed_data';

/// A simple set of metadata for a media item suitable for display. This can be
/// created using the Builder or retrieved from existing metadata using
/// [MediaMetadata.getDescription].
class MediaDescription {
  final String mediaId;
  final String title;
  final String subTitle;
  final String description;
  final Uint8List iconBitmap;
  final Uri iconUri;
  final Map extras;

//<editor-fold desc="Data Methods" defaultstate="collapsed">

  const MediaDescription({
    this.mediaId,
    this.title,
    this.subTitle,
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
          subTitle == other.subTitle &&
          description == other.description &&
          iconBitmap == other.iconBitmap &&
          iconUri == other.iconUri &&
          extras == other.extras);

  @override
  int get hashCode =>
      mediaId.hashCode ^
      title.hashCode ^
      subTitle.hashCode ^
      description.hashCode ^
      iconBitmap.hashCode ^
      iconUri.hashCode ^
      extras.hashCode;

  @override
  String toString() {
    return 'MediaDescription{' +
        ' mediaId: $mediaId,' +
        ' title: $title,' +
        ' subTitle: $subTitle,' +
        ' description: $description,' +
        ' iconBitmap: $iconBitmap,' +
        ' iconUri: $iconUri,' +
        ' extras: $extras,' +
        '}';
  }

  MediaDescription copy({
    String mediaId,
    String title,
    String subTitle,
    String description,
    Uint8List iconBitmap,
    Uri iconUri,
    Map extras,
  }) {
    return MediaDescription(
      mediaId: mediaId ?? this.mediaId,
      title: title ?? this.title,
      subTitle: subTitle ?? this.subTitle,
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
      keyMapper('subTitle'): this.subTitle,
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
      subTitle: map[keyMapper('subTitle')] as String,
      description: map[keyMapper('description')] as String,
      iconBitmap: map[keyMapper('iconBitmap')] as Uint8List,
      iconUri: map[keyMapper('iconUri')] as Uri,
      extras: map[keyMapper('extras')] as Map,
    );
  }

//</editor-fold>
}
