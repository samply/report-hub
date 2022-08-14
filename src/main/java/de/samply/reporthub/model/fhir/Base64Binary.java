package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Objects;
import java.util.Optional;

public record Base64Binary(Optional<String> value) implements Element {

  private static final Encoder ENCODER = Base64.getEncoder();
  private static final Decoder DECODER = Base64.getDecoder();

  public Base64Binary {
    Objects.requireNonNull(value);
  }

  public Optional<byte[]> decodedValue() {
    return value.map(DECODER::decode);
  }

  @JsonValue
  public String jsonValue() {
    return value.orElse(null);
  }

  public static Base64Binary encoded(byte[] bytes) {
    return valueOf(ENCODER.encodeToString(bytes));
  }

  public static Base64Binary valueOf(String value) {
    return Base64Binary.builder().withValue(value).build();
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

    public Base64Binary build() {
      return new Base64Binary(Optional.ofNullable(value));
    }
  }

  @JsonCreator
  public static Base64Binary jsonCreator(JsonNode node) {
    if (node.isTextual()) {
      return Base64Binary.valueOf(node.textValue());
    }
    throw new RuntimeException("Invalid JSON node `%s` for Code type.".formatted(node));
  }
}
