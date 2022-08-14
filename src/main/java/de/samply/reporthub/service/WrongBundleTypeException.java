package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.BundleType;
import de.samply.reporthub.model.fhir.Code;

public final class WrongBundleTypeException extends Exception {

  public WrongBundleTypeException(BundleType expected, Code actual) {
    super("Expect Bundle type to be `%s` but was `%s`.".formatted(expected,
        actual.value().orElse("<unknown>")));
  }
}
