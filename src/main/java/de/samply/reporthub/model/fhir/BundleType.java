package de.samply.reporthub.model.fhir;

import java.util.function.Predicate;

public enum BundleType implements Predicate<Code> {

  MESSAGE("message"),
  TRANSACTION("transaction"),
  BATCH("batch");

  private final String code;

  BundleType(String code) {
    this.code = code;
  }

  public Code code() {
    return Code.valueOf(code);
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
