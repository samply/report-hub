package de.samply.reporthub.model.fhir;

/**
 * Codes of the CodeSystem {@code http://hl7.org/fhir/issue-severity}.
 */
public enum IssueSeverity {

  FATAL("Fatal"),
  ERROR("error"),
  WARNING("Warning"),
  INFORMATION("Information");

  private final Code code;

  IssueSeverity(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
