package de.samply.reporthub.service.fhir.store;

import de.samply.reporthub.model.fhir.OperationOutcome;
import java.util.Objects;

/**
 * Exception for {@code BadRequest} with {@link OperationOutcome}.
 */
public final class BadRequestException extends Exception {

  private final OperationOutcome operationOutcome;

  public BadRequestException(String message) {
    this(message, OperationOutcome.empty());
  }

  public BadRequestException(String message, OperationOutcome operationOutcome) {
    super(message);
    this.operationOutcome = Objects.requireNonNull(operationOutcome);
  }

  public OperationOutcome operationOutcome() {
    return operationOutcome;
  }
}
