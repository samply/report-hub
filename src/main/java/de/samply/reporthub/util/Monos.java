package de.samply.reporthub.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import reactor.core.publisher.Mono;

public interface Monos {

  static <T, U, R> Mono<R> map(Mono<T> mono1, Mono<U> mono2,
      BiFunction<? super T, ? super U, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return mono1.flatMap(v1 -> mono2.map(v2 -> mapper.apply(v1, v2)));
  }

  static <T, U, R> Mono<R> map(Mono<T> mono1, Function<T, Mono<U>> mono2,
      BiFunction<? super T, ? super U, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return mono1.flatMap(v1 -> mono2.apply(v1).map(v2 -> mapper.apply(v1, v2)));
  }
}
