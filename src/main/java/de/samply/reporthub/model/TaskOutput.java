package de.samply.reporthub.model;

import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Coding;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Codes of the CodeSystem {@code https://dktk.dkfz.de/fhir/CodeSystem/task-output}.
 */
public enum TaskOutput implements Predicate<Coding> {

  MEASURE_REPORT("measure-report"),
  ERROR("error");

  public static final String CODE_SYSTEM_URL = "https://dktk.dkfz.de/fhir/CodeSystem/task-output";

  private final String code;

  TaskOutput(String code) {
    this.code = Objects.requireNonNull(code);
  }

  public String code() {
    return code;
  }

  public Coding coding() {
    return Coding.of(CODE_SYSTEM_URL, code);
  }


  public Predicate<CodeableConcept> codeableConceptPredicate() {
    return CodeableConcept.containsCoding(CODE_SYSTEM_URL, code);
  }

  @Override
  public boolean test(Coding coding) {
    return coding.hasSystemValue1(CODE_SYSTEM_URL) && coding.hasCodeValue1(code);
  }
}
