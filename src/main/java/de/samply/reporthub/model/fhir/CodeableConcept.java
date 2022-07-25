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

  public boolean containsCoding(String codeValue) {
    return containsCoding(Coding.hasCodeValue(codeValue));
  }

  public boolean containsCoding(Predicate<Coding> predicate) {
    return coding.stream().anyMatch(predicate);
  }

  /**
   * Returns a predicate that can be used to find codeable concepts with the given {@code system}
   * and {@code codeValue}.
   *
   * @param system the system to match
   * @param codeValue the code value to match
   * @return a predicate
   */
  public static Predicate<CodeableConcept> containsCoding(String system, String codeValue) {
    return codeableConcept -> codeableConcept.containsCoding(Coding.hasSystem(system).and(Coding.hasCodeValue(codeValue)));
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
