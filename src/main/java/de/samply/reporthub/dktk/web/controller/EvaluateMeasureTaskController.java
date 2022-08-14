package de.samply.reporthub.dktk.web.controller;

import static de.samply.reporthub.dktk.model.fhir.TaskInput.MEASURE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import de.samply.reporthub.Util;
import de.samply.reporthub.dktk.model.fhir.TaskOutput;
import de.samply.reporthub.dktk.web.model.EvaluateMeasureTask;
import de.samply.reporthub.dktk.web.model.EvaluateMeasureTask.HistoryListItem;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Measure;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.StringElement;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Output;
import de.samply.reporthub.service.fhir.store.DataStore;
import de.samply.reporthub.service.fhir.store.ResourceNotFoundException;
import de.samply.reporthub.service.fhir.store.TaskStore;
import de.samply.reporthub.util.Optionals;
import de.samply.reporthub.web.model.Link;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
public class EvaluateMeasureTaskController {

  private static final Logger logger = LoggerFactory.getLogger(EvaluateMeasureTaskController.class);

  private static final Predicate<CodeableConcept> MEASURE_CONCEPT =
      CodeableConcept.containsCoding(MEASURE);

  public static final Predicate<CodeableConcept> MEASURE_REPORT_CONCEPT =
      CodeableConcept.containsCoding(TaskOutput.MEASURE_REPORT);

  public static final Predicate<CodeableConcept> ERROR_CONCEPT =
      CodeableConcept.containsCoding(TaskOutput.ERROR);

  private final TaskStore taskStore;
  private final DataStore dataStore;

  public EvaluateMeasureTaskController(TaskStore taskStore, DataStore dataStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.dataStore = Objects.requireNonNull(dataStore);
  }

  /**
   * Produces the router function for the {@code task/evaluate-measure/{id}} endpoint.
   *
   * @return the router function for the {@code task/evaluate-measure/{id}} endpoint
   */
  @Bean
  public RouterFunction<ServerResponse> taskRouter() {
    return route(GET("task/evaluate-measure/{id}"), this::handle);
  }

  Mono<ServerResponse> handle(ServerRequest request) {
    String id = request.pathVariable("id");
    logger.debug("Request Task with id: {}", id);
    return task(request, id)
        .flatMap(task -> ok().render("dktk/evaluate-measure-task", Map.of("task", task)))
        .onErrorResume(ResourceNotFoundException.class, EvaluateMeasureTaskController::notFound);
  }

  Mono<EvaluateMeasureTask> task(ServerRequest request, String id) {
    return taskStore.fetchTask(id)
        .flatMap(task -> taskHistory(id)
            .flatMap(history -> measureUrl(task)
                .map(url -> new MeasureLinkBuilder(request).build(url)
                    .map(measureLink ->
                        new TaskBuilder(request).build(task, Optional.of(measureLink), history)))
                .orElseGet(() ->
                    Mono.just(new TaskBuilder(request).build(task, Optional.empty(), history)))));
  }

  private Mono<List<Task>> taskHistory(String taskId) {
    return taskStore.fetchTaskHistory(taskId)
        .onErrorResume(e -> Flux.empty())
        .collectList();
  }

  private Optional<String> measureUrl(Task task) {
    return task.findInput(MEASURE_CONCEPT)
        .flatMap(input -> input.castValue(Canonical.class))
        .flatMap(Canonical::value);
  }

  private class MeasureLinkBuilder {

    private final ServerRequest request;

    private MeasureLinkBuilder(ServerRequest request) {
      this.request = Objects.requireNonNull(request);
    }

    private Mono<Link> build(String url) {
      return dataStore.findByUrl(Measure.class, url)
          .map(this::measureLink)
          .onErrorResume(e -> Mono.just(Link.of(URI.create(url), url)));
    }

    private Link measureLink(Measure measure) {
      return Link.of(measure.id().map(this::measureUri).orElse(URI.create("#")),
          measure.title().orElse(measure.name().orElse("Measure")));
    }

    private URI measureUri(String id) {
      return request.uriBuilder().replacePath("dktk/measure/{id}").build(id);
    }
  }

  private record TaskBuilder(ServerRequest request) {

    private TaskBuilder {
      Objects.requireNonNull(request);
    }

    private EvaluateMeasureTask build(Task task, Optional<Link> measureLink, List<Task> history) {
      return new EvaluateMeasureTask(
          task.id().orElse("unknown"),
          task.status().value().orElse("unknown"),
          measureLink,
          task.findOutput(MEASURE_REPORT_CONCEPT).flatMap(this::reportLink),
          task.findOutput(ERROR_CONCEPT)
              .flatMap(o -> o.castValue(StringElement.class))
              .flatMap(StringElement::value),
          history.stream().flatMap(t -> convertHistoryLineItem(t).stream()).toList());
    }

    private Optional<HistoryListItem> convertHistoryLineItem(Task task) {
      return Optionals.map(task.lastModified(), task.status().value(),
          (lastModified, status) -> new HistoryListItem(lastModified, status,
              task.findOutput(MEASURE_REPORT_CONCEPT).flatMap(this::reportLink)));
    }

    Optional<Link> reportLink(Output output) {
      return output.castValue(Reference.class)
          .flatMap(Reference::reference)
          .flatMap(Util::referenceId)
          .map(this::reportLink);
    }

    private Link reportLink(String id) {
      return Link.of(request.uriBuilder().replacePath("exliquid-report/{id}").build(id), "Report");
    }
  }

  private static Mono<ServerResponse> notFound(ResourceNotFoundException e) {
    var error = "The Task with id `%s` was not found.".formatted(e.id());
    logger.warn(error);
    return ok().render("404", Map.of("error", error));
  }
}
