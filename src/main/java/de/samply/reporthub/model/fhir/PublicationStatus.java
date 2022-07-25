package de.samply.reporthub.model.fhir;

public enum PublicationStatus {

  DRAFT("draft"),
  ACTIVE("active"),
  RETIRED("retired"),
  UNKNOWN("unknown");

  private final Code code;

  PublicationStatus(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
