enum PlayMode {
  sequence,
  shuffle,
  single,
}

PlayMode parsePlayMode(String name) {
  switch (name?.toLowerCase() ?? "") {
    case "shuffle":
      return PlayMode.shuffle;
    case "single":
      return PlayMode.single;
    default:
      return PlayMode.sequence;
  }
}

String playModeToStr(PlayMode mode) {
  switch (mode) {
    case PlayMode.shuffle:
      return "shuffle";
    case PlayMode.sequence:
      return "sequence";
    case PlayMode.single:
      return "single";
  }
  throw "can not reach";
}
