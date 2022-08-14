package de.samply.reporthub.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface Optionals {

  /**
   * If the values of both Optionals are present, returns an {@code Optional} describing (as if by
   * {@link Optional#ofNullable}) the result of applying the given mapping function to both values,
   * otherwise returns an empty {@code Optional}.
   *
   * <p>If the mapping function returns a {@code null} result then this method returns an empty
   * {@code Optional}.
   *
   * @param optional1 the first Optional
   * @param optional2 the second Optional
   * @param mapper    the mapping function to apply to both values, if both are present
   * @param <T>       the type of the first value
   * @param <U>       the type of the second value
   * @param <R>       the type of the value returned from the mapping function
   * @return an {@code Optional} describing the result of applying a mapping function to the values
   * of both Optionals, if both value are present, otherwise an empty {@code Optional}
   * @throws NullPointerException if the mapping function is {@code null}
   */
  static <T, U, R> Optional<R> map(Optional<T> optional1, Optional<U> optional2,
      BiFunction<? super T, ? super U, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return optional1.flatMap(v1 -> optional2.map(v2 -> mapper.apply(v1, v2)));
  }

  static <T, U, R> R orElseGet(Optional<T> optional1, Optional<U> optional2,
      BiFunction<? super T, ? super U, R> mapper, Supplier<? extends R> orElseGet1,
      Supplier<? extends R> orElseGet2) {
    Objects.requireNonNull(mapper);
    return optional1.map(v1 -> optional2.map(v2 -> mapper.apply(v1, v2)).orElseGet(orElseGet2))
        .orElseGet(orElseGet1);
  }

  static <T, U, R> R orElseGet(Optional<T> optional1,
      Supplier<? extends R> orElseGet1, Function<? super T, ? extends Optional<? extends U>> f2,
      Supplier<? extends R> orElseGet2,
      BiFunction<? super T, ? super U, R> mapper) {
    Objects.requireNonNull(mapper);
    return optional1.map(v1 -> f2.apply(v1).map(v2 -> mapper.apply(v1, v2)).orElseGet(orElseGet2))
        .orElseGet(orElseGet1);
  }

  static <T, U, V, R> R orElseGet(Optional<T> optional1,
      Supplier<? extends R> orElseGet1,
      Function<? super T, ? extends Optional<? extends U>> f2,
      Supplier<? extends R> orElseGet2,
      Function<? super T, ? extends Optional<? extends V>> f3,
      Supplier<? extends R> orElseGet3,
      TriFunction<? super T, ? super U, ? super V, R> mapper) {
    Objects.requireNonNull(mapper);
    return optional1
        .map(v1 -> f2.apply(v1)
            .map(v2 -> f3.apply(v1)
                .map(v3 -> mapper.apply(v1, v2, v3)
                ).orElseGet(orElseGet3)
            ).orElseGet(orElseGet2)
        ).orElseGet(orElseGet1);
  }

  /**
   * If the values of all three Optionals are present, returns an {@code Optional} describing (as if
   * by {@link Optional#ofNullable}) the result of applying the given mapping function to all three
   * values, otherwise returns an empty {@code Optional}.
   *
   * <p>If the mapping function returns a {@code null} result then this method returns an empty
   * {@code Optional}.
   *
   * @param optional1 the first Optional
   * @param optional2 the second Optional
   * @param optional3 the third Optional
   * @param mapper    the mapping function to apply to all three values, if all three values are
   *                  present
   * @param <T>       the type of the first value
   * @param <U>       the type of the second value
   * @param <V>       the type of the third value
   * @param <R>       the type of the value returned from the mapping function
   * @return an {@code Optional} describing the result of applying a mapping function to the value
   * of all three Optionals, if all three values are present, otherwise an empty {@code Optional}
   * @throws NullPointerException if the mapping function is {@code null}
   */
  static <T, U, V, R> Optional<R> map(Optional<T> optional1, Optional<U> optional2,
      Optional<V> optional3, TriFunction<? super T, ? super U, ? super V, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return optional1.flatMap(v1 -> optional2.flatMap(v2 -> optional3.map(v3 ->
        mapper.apply(v1, v2, v3))));
  }

  static <T, U, V, W, R> Optional<R> map(Optional<T> optional1, Optional<U> optional2,
      Optional<V> optional3, Optional<W> optional4,
      QuadFunction<? super T, ? super U, ? super V, ? super W, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return optional1.flatMap(v1 -> optional2.flatMap(v2 -> optional3.flatMap(v3 ->
        optional4.map(v4 -> mapper.apply(v1, v2, v3, v4)))));
  }

  /**
   * If the values of both Optionals are present, returns the result of applying the given
   * {@code Optional}-bearing mapping function to both values, otherwise returns an empty
   * {@code Optional}.
   *
   * <p>This method is similar to {@link Optionals#map(Optional, Optional, BiFunction)}, but the
   * mapping function is one whose result is already an {@code Optional}, and if invoked,
   * {@code flatMap} does not wrap it within an additional {@code Optional}.
   *
   * @param optional1 the first Optional
   * @param optional2 the second Optional
   * @param mapper    the mapping function to apply to both values, if both are present
   * @param <T>       the type of the first value
   * @param <U>       the type of the second value
   * @param <R>       the type of the value of the {@code Optional} returned from the mapping
   *                  function
   * @return the result of applying an {@code Optional}-bearing mapping function to the values of
   * both Optionals, if both values are present, otherwise an empty {@code Optional}
   * @throws NullPointerException if the mapping function is {@code null}or returns a {@code null}
   *                              result
   */
  static <T, U, R> Optional<R> flatMap(Optional<T> optional1, Optional<U> optional2,
      BiFunction<? super T, ? super U, ? extends Optional<? extends R>> mapper) {
    Objects.requireNonNull(mapper);
    return optional1.flatMap(v1 -> optional2.flatMap(v2 -> mapper.apply(v1, v2)));
  }

  static <T, U, V, R> Optional<R> flatMap(Optional<T> optional1, Optional<U> optional2,
      Optional<V> optional3, TriFunction<T, U, V, Optional<R>> mapper) {
    Objects.requireNonNull(mapper);
    return optional1.flatMap(v1 -> optional2.flatMap(v2 -> optional3.flatMap(v3 ->
        mapper.apply(v1, v2, v3))));
  }

  static <T, U, V, W, R> Optional<R> flatMap(Optional<T> optional1, Optional<U> optional2,
      Optional<V> optional3, Optional<W> optional4,
      QuadFunction<? super T, ? super U, ? super V, ? super W, ? extends Optional<? extends R>> mapper) {
    Objects.requireNonNull(mapper);
    return optional1.flatMap(v1 -> optional2.flatMap(v2 -> optional3.flatMap(v3 ->
        optional4.flatMap(v4 -> mapper.apply(v1, v2, v3, v4)))));
  }
}
