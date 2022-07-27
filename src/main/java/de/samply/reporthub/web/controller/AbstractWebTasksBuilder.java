package de.samply.reporthub.web.controller;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.Task.Output;
import de.samply.reporthub.web.model.Link;
import java.util.Objects;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

class AbstractWebTasksBuilder {

  private final UriComponentsBuilder uriBuilder;

  AbstractWebTasksBuilder(UriComponentsBuilder uriBuilder) {
    this.uriBuilder = Objects.requireNonNull(uriBuilder);
  }

  Optional<Link> activityDefinitionLink(ActivityDefinition activityDefinition) {
    return activityDefinition.id()
        .flatMap(id -> activityDefinition.title()
            .map(title -> activityDefinitionLink(id, title)));
  }

  private Link activityDefinitionLink(String id, String title) {
    return new Link(uriBuilder.cloneBuilder().path("activity-definition/{id}").build(id), title);
  }

  Optional<Link> reportLink(Output output) {
    return output.castValue(Reference.class)
        .flatMap(Reference::reference)
        .flatMap(Util::referenceId)
        .map(this::reportLink);
  }

  private Link reportLink(String id) {
    return new Link(uriBuilder.cloneBuilder().path("exliquid-report/{id}").build(id), "Report");
  }
}
