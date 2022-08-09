package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.samply.reporthub.Util;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@JsonInclude(Include.NON_EMPTY)
public record CodeableConcept(List<Coding> coding, Optional<String> text) implements Element {

  public static Predicate<CodeableConcept> containsCoding(Predicate<Coding> predicate) {
    return codeableConcept -> codeableConcept.coding.stream().anyMatch(predicate);
  }

  /**
   * Returns a predicate that can be used to find codeable concepts with the given
   * {@code systemValue} and {@code codeValue}.
   *
   * @param systemValue the system value to match
   * @param codeValue   the code value to match
   * @return a predicate
   */
  public static Predicate<CodeableConcept> containsCoding(String systemValue, String codeValue) {
    return containsCoding(Coding.hasSystemValue(systemValue).and(Coding.hasCodeValue(codeValue)));
  }

  public static CodeableConcept of(Coding coding) {
    return new Builder().withCoding(List.of(coding)).build();
  }

  public static CodeableConcept of(String text) {
    return new Builder().withText(text).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private List<Coding> coding;
    private String text;

    public Builder withCoding(List<Coding> coding) {
      this.coding = coding;
      return this;
    }

    public Builder withText(String text) {
      this.text = Objects.requireNonNull(text);
      return this;
    }

    public CodeableConcept build() {
      return new CodeableConcept(Util.copyOfNullable(coding), Optional.ofNullable(text));
    }
  }
}
