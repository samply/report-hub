package de.samply.reporthub.model.fhir;


public enum TaskStatus {

  DRAFT("draft"),
  REQUESTED("requested"),
  RECEIVED("received"),
  ACCEPTED("accepted"),
  REJECTED("rejected"),
  READY("ready"),
  CANCELLED("cancelled"),
  IN_PROGRESS("in-progress"),
  ON_HOLD("on-hold"),
  FAILED("failed"),
  COMPLETED("completed"),
  ENTERED_IN_ERROR("entered-in-error");

  private final Code code;

  TaskStatus(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
