package de.samply.reporthub.service.fhir.store;

import de.samply.reporthub.model.fhir.OperationOutcome;
import java.util.Objects;

/**
 * Exception for {@code NotFound} with {@link OperationOutcome}.
 */
public final class NotFoundException extends Exception {

  private final OperationOutcome operationOutcome;

  public NotFoundException(String message) {
    this(message, OperationOutcome.empty());
  }

  public NotFoundException(String message, OperationOutcome operationOutcome) {
    super(message);
    this.operationOutcome = Objects.requireNonNull(operationOutcome);
  }

  public OperationOutcome operationOutcome() {
    return operationOutcome;
  }
}
