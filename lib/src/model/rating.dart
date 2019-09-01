class Rating {
  /// Indicates a rating style is not supported. A Rating will never have this
  /// type, but can be used by other classes to indicate they do not support
  /// Rating.
  static const RATING_NONE = 0;

  /// A rating style with a single degree of rating, "heart" vs "no heart". Can be used to
  /// indicate the content referred to is a favorite (or not).
  static const RATING_HEART = 1;

  /// A rating style for "thumb up" vs "thumb down".
  static const RATING_THUMB_UP_DOWN = 2;

  /// A rating style with 0 to 3 stars.
  static const RATING_3_STARS = 3;

  /// A rating style with 0 to 4 stars.
  static const RATING_4_STARS = 4;

  /// A rating style with 0 to 5 stars.
  static const RATING_5_STARS = 5;

  /// A rating style expressed as a percentage.
  static const RATING_PERCENTAGE = 6;

  static const RATING_NOT_RATED = -1.0;

  final int _ratingStyle;
  final double _ratingValue;

  Rating._private(this._ratingStyle, this._ratingValue);

  /// Return a Rating instance with no rating.
  /// Create and return a new Rating instance with no rating known for the given
  /// rating style.
  factory Rating.newUnratedRating(int ratingStyle) {
    switch (ratingStyle) {
      case RATING_HEART:
      case RATING_THUMB_UP_DOWN:
      case RATING_3_STARS:
      case RATING_4_STARS:
      case RATING_5_STARS:
      case RATING_PERCENTAGE:
        return Rating._private(ratingStyle, RATING_NOT_RATED);
      default:
        throw Exception();
    }
  }

  /// Return a Rating instance with a heart-based rating.
  /// Create and return a new Rating instance with a rating style of {@link #RATING_HEART},
  /// and a heart-based rating.
  factory Rating.newHeartRating(bool hasHeart) {
    return Rating._private(RATING_HEART, hasHeart ? 1.0 : 0.0);
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Rating &&
          runtimeType == other.runtimeType &&
          _ratingStyle == other._ratingStyle &&
          _ratingValue == other._ratingValue);

  @override
  int get hashCode => _ratingStyle.hashCode ^ _ratingValue.hashCode;

  @override
  String toString() {
    return 'Rating{' + ' ratingStyle: $_ratingStyle,' + ' ratingValue: $_ratingValue,' + '}';
  }

  Map<String, dynamic> toMap() {
    return {
      'ratingStyle': this._ratingStyle,
      'ratingValue': this._ratingValue,
    };
  }

  factory Rating.fromMap(Map<String, dynamic> map) {
    if (map == null) return null;
    return new Rating._private(
      map['ratingStyle'] as int,
      map['ratingValue'] as double,
    );
  }
}
