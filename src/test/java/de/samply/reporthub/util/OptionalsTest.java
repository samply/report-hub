package de.samply.reporthub.util;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.util.OptionalValuesSource.Value;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class OptionalsTest {

  @Test
  void map_present() {
    var pair = Optionals.map(Optional.of("v1"), Optional.of("v2"), Pair::of);

    assertThat(pair).isPresent();
    assertThat(pair.map(Pair::v1)).contains("v1");
    assertThat(pair.map(Pair::v2)).contains("v2");
  }

  @Test
  void map_returningNull() {
    var optional = Optionals.map(Optional.of("v1"), Optional.of("v2"), (v1, v2) -> null);

    assertThat(optional).isEmpty();
  }

  @ParameterizedTest
  @OptionalValuesSource({@Value({"", "v2"}), @Value({"v1", ""}), @Value({"", ""})})
  void map_empty(Optional<String> o1, Optional<String> o2) {
    var pair = Optionals.map(o1, o2, Pair::of);

    assertThat(pair).isEmpty();
    assertThat(pair.map(Pair::v1)).isEmpty();
    assertThat(pair.map(Pair::v2)).isEmpty();
  }

  @Test
  void map3_present() {
    var triple = Optionals.map(Optional.of("v1"), Optional.of("v2"), Optional.of("v3"), Triple::of);

    assertThat(triple).isPresent();
    assertThat(triple.map(Triple::v1)).contains("v1");
    assertThat(triple.map(Triple::v2)).contains("v2");
    assertThat(triple.map(Triple::v3)).contains("v3");
  }

  @Test
  void map3_returningNull() {
    var optional = Optionals.map(Optional.of("v1"), Optional.of("v2"), Optional.of("v3"),
        (v1, v2, v3) -> null);

    assertThat(optional).isEmpty();
  }

  @ParameterizedTest
  @OptionalValuesSource({
      @Value({"", "v2", "v3"}),
      @Value({"v1", "", "v3"}),
      @Value({"v1", "v2", ""}),
      @Value({"", "", "v3"}),
      @Value({"v1", "", ""}),
      @Value({"", "v2", ""}),
      @Value({"", "", ""})})
  void map3_empty(Optional<String> o1, Optional<String> o2, Optional<String> o3) {
    var triple = Optionals.map(o1, o2, o3, Triple::of);

    assertThat(triple).isEmpty();
    assertThat(triple.map(Triple::v1)).isEmpty();
    assertThat(triple.map(Triple::v2)).isEmpty();
    assertThat(triple.map(Triple::v3)).isEmpty();
  }

  @Test
  void flatMap_present() {
    var pair = Optionals.flatMap(Optional.of("v1"), Optional.of("v2"),
        (v1, v2) -> Optional.of(Pair.of(v1, v2)));

    assertThat(pair).isPresent();
    assertThat(pair.map(Pair::v1)).contains("v1");
    assertThat(pair.map(Pair::v2)).contains("v2");
  }

  @ParameterizedTest
  @OptionalValuesSource({@Value({"", "v2"}), @Value({"v1", ""}), @Value({"", ""})})
  void flatMap_empty(Optional<String> o1, Optional<String> o2) {
    var pair = Optionals.flatMap(o1, o2, (v1, v2) -> Optional.of(Pair.of(v1, v2)));

    assertThat(pair).isEmpty();
    assertThat(pair.map(Pair::v1)).isEmpty();
    assertThat(pair.map(Pair::v2)).isEmpty();
  }

  @Test
  void flatMap3_present() {
    var triple = Optionals.flatMap(Optional.of("v1"), Optional.of("v2"), Optional.of("v3"),
        (v1, v2, v3) -> Optional.of(Triple.of(v1, v2, v3)));

    assertThat(triple).isPresent();
    assertThat(triple.map(Triple::v1)).contains("v1");
    assertThat(triple.map(Triple::v2)).contains("v2");
    assertThat(triple.map(Triple::v3)).contains("v3");
  }

  @ParameterizedTest
  @OptionalValuesSource({
      @Value({"", "v2", "v3"}),
      @Value({"v1", "", "v3"}),
      @Value({"v1", "v2", ""}),
      @Value({"", "", "v3"}),
      @Value({"v1", "", ""}),
      @Value({"", "v2", ""}),
      @Value({"", "", ""})})
  void flatMap3_empty(Optional<String> o1, Optional<String> o2, Optional<String> o3) {
    var triple = Optionals.flatMap(o1, o2, o3, (v1, v2, v3) -> Optional.of(Triple.of(v1, v2, v3)));

    assertThat(triple).isEmpty();
    assertThat(triple.map(Triple::v1)).isEmpty();
    assertThat(triple.map(Triple::v2)).isEmpty();
    assertThat(triple.map(Triple::v3)).isEmpty();
  }
}
