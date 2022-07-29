package de.samply.reporthub.util;

public record IntPair(int v1, int v2) {

  public static final IntPair ZERO = new IntPair(0, 0);

  public static IntPair of(int v1, int v2) {
    return new IntPair(v1, v2);
  }

  public IntPair plus(IntPair other) {
    return new IntPair(v1 + other.v1, v2 + other.v2);
  }
}
