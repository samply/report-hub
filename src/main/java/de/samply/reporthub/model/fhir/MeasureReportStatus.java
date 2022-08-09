package de.samply.reporthub.model.fhir;

public enum MeasureReportStatus {

  COMPLETE("complete"),
  PENDING("pending"),
  ERROR("error");

  private final Code code;

  MeasureReportStatus(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
