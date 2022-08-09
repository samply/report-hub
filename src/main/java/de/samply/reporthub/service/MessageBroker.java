package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.Bundle;
import java.util.function.Predicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageBroker {

  Mono<Void> send(Bundle message);

  Flux<Record> receive(Predicate<Bundle> messagePredicate);
}
