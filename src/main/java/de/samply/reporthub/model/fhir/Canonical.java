package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;

public record Canonical(Optional<String> value) implements Element {

  public Canonical {
    Objects.requireNonNull(value);
  }

  public boolean hasValue(String value) {
    return this.value.equals(Optional.of(value));
  }

  @JsonValue
  public String jsonValue() {
    return value.orElse(null);
  }

  public static Canonical valueOf(String value) {
    return Canonical.builder().withValue(value).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String value;

    private Builder() {
    }

    public Builder withValue(String value) {
      this.value = Objects.requireNonNull(value);
      return this;
    }

    public Canonical build() {
      return new Canonical(Optional.ofNullable(value));
    }
  }

  @JsonCreator
  public static Canonical jsonCreator(JsonNode node) {
    if (node.isTextual()) {
      return Canonical.valueOf(node.textValue());
    }
    throw new RuntimeException("Invalid JSON node `%s` for Code type.".formatted(node));
  }
}
