package de.samply.reporthub.service;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Task;
import reactor.core.publisher.Mono;

public interface TaskCreator {

  Mono<Task> create(ActivityDefinition activityDefinition);
}
