package de.samply.reporthub.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface Monos {

  static <T> Mono<T> justOrError(Optional<T> optional, Supplier<Throwable> supplier) {
    return optional.map(Mono::just).orElseGet(() -> Mono.error(supplier.get()));
  }

  static <T, U, R> Mono<R> map(Mono<T> mono1, Mono<U> mono2,
      BiFunction<? super T, ? super U, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return mono1.flatMap(v1 -> mono2.map(v2 -> mapper.apply(v1, v2)));
  }

  static <T, U, V, R> Mono<R> map(Mono<T> mono1, Mono<U> mono2, Mono<V> mono3,
      TriFunction<? super T, ? super U, ? super V, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return mono1.flatMap(v1 -> mono2.flatMap(v2 -> mono3.map(v3 -> mapper.apply(v1, v2, v3))));
  }

  static <T, U, V, W, R> Mono<R> map(Mono<T> mono1, Mono<U> mono2, Mono<V> mono3, Mono<W> mono4,
      QuadFunction<? super T, ? super U, ? super V, ? super W, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return mono1.flatMap(v1 -> mono2.flatMap(v2 -> mono3.flatMap(v3 -> mono4
        .map(v4 -> mapper.apply(v1, v2, v3, v4)))));
  }

  static <T, U, R> Mono<R> map(Mono<T> mono1, Function<T, Mono<U>> f2,
      BiFunction<? super T, ? super U, ? extends R> mapper) {
    Objects.requireNonNull(f2);
    Objects.requireNonNull(mapper);
    return mono1.flatMap(v1 -> f2.apply(v1).map(v2 -> mapper.apply(v1, v2)));
  }

  static <T, U, R> Mono<R> flatMap(Mono<T> mono1, Mono<U> mono2,
      BiFunction<? super T, ? super U, ? extends Mono<? extends R>> mapper) {
    Objects.requireNonNull(mapper);
    return mono1.flatMap(v1 -> mono2.flatMap(v2 -> mapper.apply(v1, v2)));
  }
}
