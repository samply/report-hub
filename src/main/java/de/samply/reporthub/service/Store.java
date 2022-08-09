package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.CapabilityStatement;
import de.samply.reporthub.model.fhir.Resource;
import reactor.core.publisher.Mono;

public interface Store {

  Mono<CapabilityStatement> fetchMetadata();

  <T extends Resource> Mono<T> fetchResource(Class<T> type, String id);
}
