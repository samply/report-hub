package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.OperationOutcome;
import java.util.Objects;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class FhirResponseException extends Exception {

  private final WebClientResponseException responseException;
  private final OperationOutcome operationOutcome;

  public FhirResponseException(WebClientResponseException responseException,
      OperationOutcome operationOutcome) {
    this.responseException = Objects.requireNonNull(responseException);
    this.operationOutcome = Objects.requireNonNull(operationOutcome);
  }

  public WebClientResponseException getResponseException() {
    return responseException;
  }

  public OperationOutcome getOperationOutcome() {
    return operationOutcome;
  }
}
