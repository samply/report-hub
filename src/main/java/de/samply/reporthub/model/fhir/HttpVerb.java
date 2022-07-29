package de.samply.reporthub.model.fhir;

public enum HttpVerb {

  GET("GET"),
  POST("POST");

  private final Code code;

  HttpVerb(String code) {
    this.code = Code.valueOf(code);
  }

  public Code code() {
    return code;
  }
}
