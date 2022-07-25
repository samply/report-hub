package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.OperationOutcome;

public class BadRequestException extends Exception {

  private final OperationOutcome operationOutcome;

  public BadRequestException(String message, OperationOutcome operationOutcome) {
    super(message);
    this.operationOutcome = operationOutcome;
  }

  public OperationOutcome getOperationOutcome() {
    return operationOutcome;
  }
}
