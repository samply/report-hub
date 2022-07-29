package de.samply.reporthub.web.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import de.samply.reporthub.service.ResourceNotFoundException;
import de.samply.reporthub.service.TaskStore;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ActivityDefinitionController {

  private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionController.class);

  private final TaskStore taskStore;

  public ActivityDefinitionController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  /**
   * Produces the router function for the {@code activity-definition/{id}} endpoint.
   *
   * @return the router function for the {@code activity-definition/{id}} endpoint
   */
  @Bean
  public RouterFunction<ServerResponse> activityDefinitionRouter() {
    return route(GET("activity-definition/{id}"), this::handle);
  }

  Mono<ServerResponse> handle(ServerRequest request) {
    String id = request.pathVariable("id");
    logger.debug("Request ActivityDefinition with id = {}", id);
    return taskStore.fetchActivityDefinition(id)
        .flatMap(task -> ok().render("activity-definition", Map.of("activityDefinition", task)))
        .onErrorResume(ResourceNotFoundException.class, ActivityDefinitionController::notFound);
  }

  private static Mono<ServerResponse> notFound(ResourceNotFoundException e) {
    var error = "The ActivityDefinition with id `%s` was not found.".formatted(e.getId());
    logger.warn(error);
    return ok().render("404", Map.of("error", error));
  }
}
