package de.samply.reporthub.model.fhir;

import java.util.function.Predicate;

/**
 * Codes of the CodeSystem {@code http://hl7.org/fhir/response-code}.
 */
public enum ResponseType implements Predicate<Code> {

  OK("ok"),
  TRANSIENT_ERROR("transient-error"),
  FATAL_ERROR("fatal-error");

  private final String code;

  ResponseType(String code) {
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
