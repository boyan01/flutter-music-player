class PlayMode {
  final int index;

  const PlayMode._internal(this.index);

  static const shuffle = PlayMode._internal(0);
  static const single = PlayMode._internal(1);
  static const sequence = PlayMode._internal(2);

  factory PlayMode.undefined(int index) {
    assert(!const [0, 1, 2].contains(index), "index can not be 0,1,2");
    return PlayMode(index);
  }

  factory PlayMode(int index) {
    if (index == 0) {
      return shuffle;
    } else if (index == 1) {
      return single;
    } else if (index == 2) {
      return sequence;
    } else {
      return PlayMode._internal(index);
    }
  }

  @override
  bool operator ==(Object other) => identical(this, other) || other is PlayMode && index == other.index;

  @override
  int get hashCode => index.hashCode;

  @override
  String toString() {
    switch (index) {
      case 0:
        return "PlayMode.shuffle";
      case 1:
        return "PlayMode.single";
      case 2:
        return "PlayMode.sequence";
      default:
        return "PlayMode.undefined($index)";
    }
  }
}
