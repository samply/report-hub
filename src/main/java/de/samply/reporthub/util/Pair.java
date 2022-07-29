package de.samply.reporthub.util;

import java.util.Objects;

public record Pair<T, U>(T v1, U v2) {

  public Pair {
    Objects.requireNonNull(v1);
    Objects.requireNonNull(v2);
  }

  public static <T, U> Pair<T, U> of(T v1, U v2) {
    return new Pair<>(v1, v2);
  }
}
