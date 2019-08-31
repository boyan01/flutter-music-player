/// A simple set of metadata for a media item suitable for display. This can be
/// created using the Builder or retrieved from existing metadata using
/// [MediaMetadata.getDescription].
class MediaDescription {
  static const EXTRA_BT_FOLDER_TYPE = "extra.BT_FOLDER_TYPE";

  static const EXTRA_DOWNLOAD_STATUS = "extra.DOWNLOAD_STATUS";

  static const STATUS_NOT_DOWNLOADED = 0;
  static const STATUS_DOWNLOADING = 1;
  static const STATUS_DOWNLOADED = 2;

  final String mediaId;
  final String title;
  final String subtitle;
  final String description;
  final Uri iconUri;
  final Map extras;
  final Uri mediaUri;

  MediaDescription({
    this.mediaId,
    this.title,
    this.subtitle,
    this.description,
    this.iconUri,
    this.extras,
    this.mediaUri,
  });

  Map<String, dynamic> toMap() {
    return {
      'mediaId': this.mediaId,
      'title': this.title,
      'subtitle': this.subtitle,
      'description': this.description,
      'iconUri': this.iconUri,
      'extras': this.extras,
      'mediaUri': this.mediaUri,
    };
  }

  factory MediaDescription.fromMap(Map map) {
    return new MediaDescription(
      mediaId: map['mediaId'] as String,
      title: map['title'] as String,
      subtitle: map['subtitle'] as String,
      description: map['description'] as String,
      iconUri: Uri.tryParse(map['iconUri'] ?? ""),
      extras: map['extras'] as Map,
      mediaUri: Uri.tryParse(map['mediaUri'] ?? ""),
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is MediaDescription &&
          runtimeType == other.runtimeType &&
          mediaId == other.mediaId &&
          title == other.title &&
          subtitle == other.subtitle &&
          description == other.description &&
          iconUri == other.iconUri &&
          extras == other.extras &&
          mediaUri == other.mediaUri;

  @override
  int get hashCode =>
      mediaId.hashCode ^
      title.hashCode ^
      subtitle.hashCode ^
      description.hashCode ^
      iconUri.hashCode ^
      extras.hashCode ^
      mediaUri.hashCode;

  @override
  String toString() {
    return 'MediaDescription{mediaId: $mediaId, title: $title, subtitle: $subtitle, description: $description, iconUri: $iconUri, extras: $extras, mediaUri: $mediaUri}';
  }
}
