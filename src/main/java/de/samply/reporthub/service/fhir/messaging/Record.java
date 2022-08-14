package de.samply.reporthub.service.fhir.messaging;

import de.samply.reporthub.model.fhir.Bundle;
import java.util.Objects;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * This class represents a record received from a {@link MessageBroker} that contains the actual
 * {@link Bundle message} and can be {@link #acknowledge() acknowledged}.
 */
public class Record {

  private final Bundle message;
  private final Supplier<Mono<Void>> acknowledger;

  private Record(Bundle message, Supplier<Mono<Void>> acknowledger) {
    this.message = Objects.requireNonNull(message);
    this.acknowledger = Objects.requireNonNull(acknowledger);
  }

  public static Record of(Bundle message, Supplier<Mono<Void>> acknowledger) {
    return new Record(message, acknowledger);
  }

  /**
   * Returns the actual message.
   *
   * @return the actual message
   */
  public Bundle message() {
    return message;
  }

  /**
   * Calling this method acknowledges this record at the message broker.
   * <p>
   * Records should be acknowledged after being processed. Acknowledged records won't be received
   * anymore.
   *
   * @return a {@code Mono} that completes after the record is acknowledged
   */
  public <T> Mono<T> acknowledge() {
    return acknowledger.get().then(Mono.empty());
  }
}
