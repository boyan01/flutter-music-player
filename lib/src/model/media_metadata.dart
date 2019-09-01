/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import 'media_description.dart';
import 'rating.dart';

class MediaMetadata {
  final String title;
  final String artist;
  final int duration;
  final String album;
  final String writer;
  final String composer;
  final String author;
  final String compilation;
  final String date;
  final int year;
  final String genre;
  final int trackNumber;
  final int numTracks;
  final int discNumber;
  final String albumArtist;
  final String artUri;
  final String albumArtUri;
  final Rating userRating;
  final Rating rating;
  final String displayTitle;
  final String displaySubtitle;
  final String displayDescription;
  final String displayIconUri;
  final String mediaId;
  final int btFolderType;
  final String mediaUri;
  final int advertisement;

  /// The download status of the media which will be used for later offline playback. It should be
  /// one of the following:
  ///
  /// [MediaDescription.STATUS_DOWNLOADED]
  /// [MediaDescription.STATUS_DOWNLOADING]
  /// [MediaDescription.STATUS_NOT_DOWNLOADED]
  ///
  final int downloadStatus;

  MediaDescription _description;

  MediaMetadata({
    this.title,
    this.artist,
    this.duration,
    this.album,
    this.writer,
    this.author,
    this.composer,
    this.compilation,
    this.date,
    this.year,
    this.genre,
    this.trackNumber,
    this.numTracks,
    this.discNumber,
    this.albumArtist,
    this.artUri,
    this.albumArtUri,
    this.userRating,
    this.rating,
    this.displayTitle,
    this.displaySubtitle,
    this.displayDescription,
    this.displayIconUri,
    this.mediaId,
    this.btFolderType,
    this.mediaUri,
    this.advertisement,
    this.downloadStatus,
  });

  MediaDescription getDescription() {
    if (_description != null) {
      return _description;
    }
    List<String> text = List(3);
    if (displayTitle == null || displayTitle.isEmpty) {
      // use whatever fields we can
      var textIndex = 0;
      var keyIndex = 0;
      final description = [title, artist, album, albumArtist, writer, author, composer];
      while (textIndex < text.length && keyIndex < description.length) {
        final next = description[keyIndex++];
        if (next != null && next.isNotEmpty) {
          text[textIndex++] = next;
        }
      }
    } else {
      // If they have a display title use only display data, otherwise use
      // our best bets
      text[0] = displayTitle;
      text[1] = displaySubtitle;
      text[2] = displayDescription;
    }

    Uri iconUri;
    // Get the best Uri we can find
    final String iconUrl = [displayIconUri, artUri, albumArtUri].firstWhere(
      (uri) => uri != null && uri.isNotEmpty,
      orElse: () => null,
    );
    if (iconUrl != null) {
      iconUri = Uri.parse(iconUrl);
    }

    Uri mediaUri;
    if (this.mediaUri != null) {
      mediaUri = Uri.parse(this.mediaUri);
    }

    final extras = {};
    if (btFolderType != null) {
      extras[MediaDescription.EXTRA_BT_FOLDER_TYPE] = btFolderType;
    }
    if (downloadStatus != null) {
      extras[MediaDescription.EXTRA_DOWNLOAD_STATUS] = downloadStatus;
    }
    _description = MediaDescription(
        mediaId: mediaId,
        title: text[0],
        subtitle: text[1],
        description: text[2],
        iconUri: iconUri,
        mediaUri: mediaUri,
        extras: extras.isEmpty ? null : extras);
    return _description;
  }

  Map<String, dynamic> toMap() {
    return {
      'title': this.title,
      'artist': this.artist,
      'duration': this.duration,
      'album': this.album,
      'writer': this.writer,
      'author': this.author,
      'composer': this.composer,
      'compilation': this.compilation,
      'date': this.date,
      'year': this.year,
      'genre': this.genre,
      'trackNumber': this.trackNumber,
      'numTracks': this.numTracks,
      'discNumber': this.discNumber,
      'albumArtist': this.albumArtist,
      'artUri': this.artUri,
      'albumArtUri': this.albumArtUri,
      'userRating': this.userRating?.toMap(),
      'rating': this.rating?.toMap(),
      'displayTitle': this.displayTitle,
      'displaySubtitle': this.displaySubtitle,
      'displayDescription': this.displayDescription,
      'displayIconUri': this.displayIconUri,
      'mediaId': this.mediaId,
      'btFolderType': this.btFolderType,
      'mediaUri': this.mediaUri,
      'advertisement': this.advertisement,
      'downloadStatus': this.downloadStatus,
    };
  }

  factory MediaMetadata.fromMap(Map map) {
    if (map == null) return null;
    //TODO extras...
    return new MediaMetadata(
      title: map['title'] as String,
      artist: map['artist'] as String,
      duration: map['duration'] as int,
      album: map['album'] as String,
      writer: map['writer'] as String,
      author: map['author'] as String,
      composer: map['composer'] as String,
      compilation: map['compilation'] as String,
      date: map['date'] as String,
      year: map['year'] as int,
      genre: map['genre'] as String,
      trackNumber: map['trackNumber'] as int,
      numTracks: map['numTracks'] as int,
      discNumber: map['discNumber'] as int,
      albumArtist: map['albumArtist'] as String,
      artUri: map['artUri'] as String,
      albumArtUri: map['albumArtUri'] as String,
      userRating: Rating.fromMap(map['userRating']),
      rating: Rating.fromMap(map['rating']),
      displayTitle: map['displayTitle'] as String,
      displaySubtitle: map['displaySubtitle'] as String,
      displayDescription: map['displayDescription'] as String,
      displayIconUri: map['displayIconUri'] as String,
      mediaId: map['mediaId'] as String,
      btFolderType: map['btFolderType'] as int,
      mediaUri: map['mediaUri'] as String,
      advertisement: map['advertisement'] as int,
      downloadStatus: map['downloadStatus'] as int,
    );
  }
}
