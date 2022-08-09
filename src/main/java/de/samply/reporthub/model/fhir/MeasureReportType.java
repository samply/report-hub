package de.samply.reporthub.model.fhir;

public enum MeasureReportType {

  INDIVIDUAL("individual"),
  SUBJECT_LIST("subject-list"),
  SUMMARY("summary"),
  DATA_COLLECTION("data-collection");

  private final Code code;

  MeasureReportType(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
