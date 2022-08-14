package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;

public record StringElement(Optional<String> value) implements Element {

  public StringElement {
    Objects.requireNonNull(value);
  }

  public boolean hasValue(String value) {
    return this.value.equals(Optional.of(value));
  }

  @JsonValue
  public String jsonValue() {
    return value.orElse(null);
  }

  public static StringElement valueOf(String value) {
    return StringElement.builder().withValue(value).build();
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

    public StringElement build() {
      return new StringElement(Optional.ofNullable(value));
    }
  }

  @JsonCreator
  public static StringElement jsonCreator(JsonNode node) {
    if (node.isTextual()) {
      return StringElement.valueOf(node.textValue());
    }
    throw new RuntimeException("Invalid JSON node `%s` for Code type.".formatted(node));
  }
}
