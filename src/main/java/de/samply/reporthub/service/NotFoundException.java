package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.OperationOutcome;
import java.util.Objects;

public class NotFoundException extends Exception {

  private final OperationOutcome operationOutcome;

  public NotFoundException(String message) {
    this(message, OperationOutcome.builder().build());
  }

  public NotFoundException(String message, OperationOutcome operationOutcome) {
    super(message);
    this.operationOutcome = Objects.requireNonNull(operationOutcome);
  }

  public OperationOutcome getOperationOutcome() {
    return operationOutcome;
  }
}
