package de.samply.reporthub.model.fhir;


import java.util.function.Predicate;

public enum TaskStatus implements Predicate<Code> {

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

  private final String code;

  TaskStatus(String code) {
    this.code = code;
  }

  public Code code() {
    return Code.valueOf(code);
  }

  public String searchToken() {
    return code;
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
