package de.samply.reporthub.model.fhir;

import java.util.Objects;
import java.util.Optional;

public record Identifier(Optional<String> system, Optional<String> value) implements Element {

  public Identifier {
    Objects.requireNonNull(system);
    Objects.requireNonNull(value);
  }

  public static Identifier of(String system, String value) {
    return new Builder().withSystem(system).withValue(value).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String system;
    private String value;

    public Builder withSystem(String system) {
      this.system = Objects.requireNonNull(system);
      return this;
    }

    public Builder withValue(String value) {
      this.value = Objects.requireNonNull(value);
      return this;
    }

    public Identifier build() {
      return new Identifier(Optional.ofNullable(system), Optional.ofNullable(value));
    }
  }
}
