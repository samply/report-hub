package de.samply.reporthub.model.fhir;

/**
 * Codes of the CodeSystem {@code http://hl7.org/fhir/issue-type}.
 */
public enum IssueType {

  // TODO: this is not complete
  PROCESSING("processing");

  private final Code code;

  IssueType(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
