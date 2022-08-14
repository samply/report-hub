package de.samply.reporthub.dktk.model.fhir;

import de.samply.reporthub.model.fhir.Coding;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Codes of the CodeSystem {@code https://dktk.dkfz.de/fhir/CodeSystem/message-event}.
 */
public enum MessageEvent implements Predicate<Coding> {

  EVALUATE_MEASURE("evaluate-measure"),
  EVALUATE_MEASURE_RESPONSE("evaluate-measure-response");

  public static final String CODE_SYSTEM_URL = "https://dktk.dkfz.de/fhir/CodeSystem/message-event";

  private final String code;

  MessageEvent(String code) {
    this.code = Objects.requireNonNull(code);
  }

  public Coding coding() {
    return Coding.of(CODE_SYSTEM_URL, code);
  }

  @Override
  public boolean test(Coding coding) {
    return coding.hasSystemValue1(CODE_SYSTEM_URL) && coding.hasCodeValue1(code);
  }
}
