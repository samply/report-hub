package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.Bundle;
import java.util.Objects;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

public record Record(Bundle message, Supplier<Mono<Void>> acknowledger) {

  public Record {
    Objects.requireNonNull(message);
    Objects.requireNonNull(acknowledger);
  }

  public static Record of(Bundle message, Supplier<Mono<Void>> acknowledger) {
    return new Record(message, acknowledger);
  }

  public Mono<Void> acknowledge() {
    return acknowledger.get();
  }
}
