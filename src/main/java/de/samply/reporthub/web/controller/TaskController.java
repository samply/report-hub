package de.samply.reporthub.web.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.service.ResourceNotFoundException;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.util.Optionals;
import de.samply.reporthub.web.model.WebTask;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This controller fetches one Task from the TaskStore and renders it including it's history.
 */
@Component
public class TaskController {

  private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

  public static final Predicate<CodeableConcept> REPORT_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-task-output", "measure-report");

  private final TaskStore taskStore;

  public TaskController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  /**
   * Produces the router function for the {@code task/{id}} endpoint.
   *
   * @return the router function for the {@code task/{id}} endpoint
   */
  @Bean
  public RouterFunction<ServerResponse> taskRouter() {
    return route(GET("task/{id}"), this::handle);
  }

  Mono<ServerResponse> handle(ServerRequest request) {
    String id = request.pathVariable("id");
    logger.debug("Request Task with id = {}", id);
    return webTask(request, id)
        .flatMap(task -> ok().render("task", Map.of("task", task)))
        .onErrorResume(ResourceNotFoundException.class, TaskController::notFound);
  }

  private static Mono<ServerResponse> notFound(ResourceNotFoundException e) {
    var error = "The Task with id `%s` was not found.".formatted(e.getId());
    logger.warn(error);
    return ok().render("404", Map.of("error", error));
  }

  Mono<WebTask> webTask(ServerRequest request, String id) {
    return taskStore.fetchTask(id).flatMap(new WebTasksBuilder(request)::convert);
  }

  private class WebTasksBuilder extends AbstractWebTasksBuilder {

    private WebTasksBuilder(ServerRequest uriBuilder) {
      super(uriBuilder);
    }

    private Mono<WebTask> convert(Task task) {
      return findActivityDefinition(task)
          .flatMap(activityDefinition -> history(activityDefinition, task)
              .flatMap(history -> convert(activityDefinition, task, history)));
    }

    private Mono<WebTask> convert(ActivityDefinition activityDefinition, Task task,
        List<WebTask> history) {
      return Mono.justOrEmpty(Optionals.map(task.id(),
          activityDefinitionLink(activityDefinition),
          task.status().value(),
          task.lastModified(),
          (id, activityDefinitionLink, status, lastModified) -> new WebTask(id,
              activityDefinitionLink, status, lastModified, task.findOutput(REPORT_PREDICATE)
              .flatMap(this::reportLink), history)));
    }

    private Mono<List<WebTask>> history(ActivityDefinition activityDefinition, Task task) {
      return Mono.justOrEmpty(task.id())
          .flatMap(id -> taskStore.fetchTaskHistory(id)
              .onErrorResume(e -> Flux.empty())
              .flatMap(historyTask -> convert(activityDefinition, historyTask, List.of()))
              .collectList());
    }
  }

  private Mono<ActivityDefinition> findActivityDefinition(Task task) {
    return Mono.justOrEmpty(task.instantiatesCanonical())
        .flatMap(taskStore::findActivityDefinitionByUrl);
  }
}
