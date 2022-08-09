package de.samply.reporthub.model;

import de.samply.reporthub.model.fhir.Coding;
import java.util.Objects;

/**
 * Codes of the CodeSystem {@code https://dktk.dkfz.de/fhir/CodeSystem/task-code}.
 */
public enum TaskCode {

  EVALUATE_MEASURE("evaluate-measure");

  public static final String CODE_SYSTEM_URL = "https://dktk.dkfz.de/fhir/CodeSystem/task-code";

  private final String code;

  TaskCode(String code) {
    this.code = Objects.requireNonNull(code);
  }

  public String code() {
    return code;
  }

  public Coding coding() {
    return Coding.of(CODE_SYSTEM_URL, code);
  }

  public String searchToken() {
    return CODE_SYSTEM_URL + "|" + code;
  }
}
