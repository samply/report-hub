package de.samply.reporthub.model.fhir;

import java.util.function.Predicate;

/**
 * Codes of the CodeSystem {@code http://hl7.org/fhir/publication-status}.
 */
public enum PublicationStatus implements Predicate<Code> {

  DRAFT("draft"),
  ACTIVE("active"),
  RETIRED("retired"),
  UNKNOWN("unknown");

  private final String code;

  PublicationStatus(String code) {
    this.code = code;
  }

  public Code code() {
    return Code.valueOf(code);
  }

  @Override
  public boolean test(Code code) {
    return code.hasValue(this.code);
  }

  @Override
  public String toString() {
    return code;
  }
}
