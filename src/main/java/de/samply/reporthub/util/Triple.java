package de.samply.reporthub.util;

import java.util.Objects;

public record Triple<T, U, V>(T v1, U v2, V v3) {

  public Triple {
    Objects.requireNonNull(v1);
    Objects.requireNonNull(v2);
    Objects.requireNonNull(v3);
  }

  public static <T, U, V> Triple<T, U, V> of(T v1, U v2, V v3) {
    return new Triple<>(v1, v2, v3);
  }
}
