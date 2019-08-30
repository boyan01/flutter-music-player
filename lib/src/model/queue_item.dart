import 'package:music_player/src/model/media_description.dart';

/// A single item that is part of the play queue. It contains a description
/// of the item and its id in the queue.
class QueueItem {
  /// This id is reserved. No items can be explicitly assigned this id.
  static const int UNKNOWN_ID = -1;

  /// The description for this item.
  final MediaDescription description;

  /// The queue id for this item.
  final int queueId;

  const QueueItem({this.description, this.queueId});

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is QueueItem &&
          runtimeType == other.runtimeType &&
          description == other.description &&
          queueId == other.queueId);

  @override
  int get hashCode => description.hashCode ^ queueId.hashCode;

  @override
  String toString() {
    return 'QueueItem{' + ' description: $description,' + ' id: $queueId,' + '}';
  }

  Map<String, dynamic> toMap() {
    return {
      'description': this.description.toMap(),
      'queueId': this.queueId,
    };
  }

  factory QueueItem.fromMap(Map map) {
    if (map == null) return null;
    return new QueueItem(
      description: MediaDescription.fromMap(map['description']),
      queueId: map['queueId'] as int,
    );
  }
}
