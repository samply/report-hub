package de.samply.reporthub.model;

import de.samply.reporthub.model.fhir.Code;
import java.util.Objects;

/**
 * Codes of the CodeSystem {@code http://hl7.org/fhir/response-code}.
 */
public enum ResponseType {

  OK("ok"),
  TRANSIENT_ERROR("transient-error"),
  FATAL_ERROR("fatal-error");

  public static final String CODE_SYSTEM_URL = "http://hl7.org/fhir/response-code";

  private final String code;

  ResponseType(String code) {
    this.code = Objects.requireNonNull(code);
  }

  public Code code() {
    return Code.valueOf(code);
  }
}
