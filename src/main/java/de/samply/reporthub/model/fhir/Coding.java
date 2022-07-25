package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.Coding.Builder;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@JsonInclude(Include.NON_EMPTY)
@JsonDeserialize(builder = Builder.class)
public record Coding(Optional<String> system, Optional<Code> code) implements Element {

  public static Predicate<Coding> hasSystem(String system) {
    return coding -> coding.system.equals(Optional.of(system));
  }

  public static Predicate<Coding> hasCodeValue(String codeValue) {
    return coding -> coding.code.map(code -> code.hasValue(codeValue)).orElse(false);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String system;
    private Code code;

    public Builder withSystem(String system) {
      this.system = Objects.requireNonNull(system);
      return this;
    }

    public Builder withCode(Code code) {
      this.code = Objects.requireNonNull(code);
      return this;
    }

    public Coding build() {
      return new Coding(Optional.ofNullable(system), Optional.ofNullable(code));
    }
  }
}
