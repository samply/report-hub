package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.CapabilityStatement;
import reactor.core.publisher.Mono;

public interface Store {

  Mono<CapabilityStatement> fetchMetadata();
}
