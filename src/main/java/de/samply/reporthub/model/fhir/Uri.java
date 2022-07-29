package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;

public record Uri(Optional<String> value) implements Element {

  public Uri {
    Objects.requireNonNull(value);
  }

  public boolean hasValue(String value) {
    return this.value.equals(Optional.of(value));
  }

  @JsonValue
  public String jsonValue() {
    return value.orElse(null);
  }

  public static Uri valueOf(String value) {
    return Uri.builder().withValue(value).build();
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

    public Uri build() {
      return new Uri(Optional.ofNullable(value));
    }
  }

  @JsonCreator
  public static Uri jsonCreator(JsonNode node) {
    if (node.isTextual()) {
      return Uri.valueOf(node.textValue());
    }
    throw new RuntimeException("Invalid JSON node `%s` for Code type.".formatted(node));
  }
}
