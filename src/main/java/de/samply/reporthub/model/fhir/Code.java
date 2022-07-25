package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public record Code(Optional<String> value) implements Element {

  public Code {
    Objects.requireNonNull(value);
  }

  public boolean hasValue(String value) {
    return this.value.equals(Optional.of(value));
  }

  @JsonValue
  public String jsonValue() {
    return value.orElse(null);
  }

  public static Code valueOf(String value) {
    return Code.builder().withValue(value).build();
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

    public Code build() {
      return new Code(Optional.ofNullable(value));
    }
  }

  @JsonCreator
  public static Code jsonCreator(JsonNode node) {
    if (node.isTextual()) {
      return Code.builder().withValue(node.textValue()).build();
    }
    throw new RuntimeException("Invalid JSON node `%s` for Code type.".formatted(node));
  }
}
