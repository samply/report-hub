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
public record Coding(Optional<Uri> system, Optional<Code> code) implements Element {

  public Coding {
    Objects.requireNonNull(system);
    Objects.requireNonNull(code);
  }

  public static Predicate<Coding> hasSystemValue(String systemValue) {
    return coding -> coding.system.map(system -> system.hasValue(systemValue)).orElse(false);
  }

  public boolean hasSystemValue1(String systemValue) {
    return system.map(system -> system.hasValue(systemValue)).orElse(false);
  }

  public static Predicate<Coding> hasCodeValue(String codeValue) {
    return coding -> coding.code.map(code -> code.hasValue(codeValue)).orElse(false);
  }

  public boolean hasCodeValue1(String codeValue) {
    return code.map(code -> code.hasValue(codeValue)).orElse(false);
  }

  public static Coding of(String systemValue, String codeValue) {
    return new Builder()
        .withSystem(Uri.valueOf(systemValue))
        .withCode(Code.valueOf(codeValue))
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Uri system;
    private Code code;

    public Builder withSystem(Uri system) {
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
