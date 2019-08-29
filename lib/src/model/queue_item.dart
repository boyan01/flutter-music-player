import 'package:music_player/src/model/media_description.dart';

/// A single item that is part of the play queue. It contains a description
/// of the item and its id in the queue.
class QueueItem {
  /// This id is reserved. No items can be explicitly assigned this id.
  static const int UNKNOWN_ID = -1;

  final MediaDescription description;
  final int id;

  const QueueItem({this.description, this.id});

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is QueueItem && runtimeType == other.runtimeType && description == other.description && id == other.id);

  @override
  int get hashCode => description.hashCode ^ id.hashCode;

  @override
  String toString() {
    return 'QueueItem{' + ' description: $description,' + ' id: $id,' + '}';
  }

  Map<String, dynamic> toMap() {
    return {
      'description': this.description,
      'id': this.id,
    };
  }

  factory QueueItem.fromMap(Map<String, dynamic> map) {
    return new QueueItem(
      description: map['description'] as MediaDescription,
      id: map['id'] as int,
    );
  }
}
