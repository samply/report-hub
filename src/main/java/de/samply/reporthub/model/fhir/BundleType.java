package de.samply.reporthub.model.fhir;

public enum BundleType {

  TRANSACTION("transaction");

  private final Code code;

  BundleType(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
