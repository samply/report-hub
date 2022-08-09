package de.samply.reporthub.web.controller;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.Task.Output;
import de.samply.reporthub.util.Optionals;
import de.samply.reporthub.web.model.Link;
import java.util.Objects;
import java.util.Optional;
import org.springframework.web.reactive.function.server.ServerRequest;

class AbstractWebTasksBuilder {

  private final ServerRequest request;

  AbstractWebTasksBuilder(ServerRequest request) {
    this.request = Objects.requireNonNull(request);
  }

  Optional<Link> activityDefinitionLink(ActivityDefinition activityDefinition) {
    return Optionals.map(activityDefinition.id(), activityDefinition.title(),
        this::activityDefinitionLink);
  }

  private Link activityDefinitionLink(String id, String title) {
    return new Link(request.uriBuilder().replacePath("activity-definition/{id}").build(id), title);
  }

  Optional<Link> reportLink(Output output) {
    return output.castValue(Reference.class)
        .flatMap(Reference::reference)
        .flatMap(Util::referenceId)
        .map(this::reportLink);
  }

  private Link reportLink(String id) {
    return new Link(request.uriBuilder().replacePath("exliquid-report/{id}").build(id), "Report");
  }
}
