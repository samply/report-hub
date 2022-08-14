package de.samply.reporthub.service.fhir.store;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.samply.reporthub.Util;
import de.samply.reporthub.dktk.model.fhir.TaskCode;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.CapabilityStatement;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.Meta;
import de.samply.reporthub.model.fhir.OperationOutcome;
import de.samply.reporthub.model.fhir.Resource;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.TaskStatus;
import de.samply.reporthub.util.Optionals;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * This class represents the FHIR server in which the tasks are managed.
 */
@Service
public class TaskStore implements Store {

  private static final Logger logger = LoggerFactory.getLogger(TaskStore.class);

  private final WebClient client;

  public TaskStore(@Qualifier("taskStoreClient") WebClient client) {
    this.client = Objects.requireNonNull(client);
  }

  public Mono<CapabilityStatement> fetchMetadata() {
    logger.debug("Fetch metadata");
    return client.get()
        .uri("/metadata")
        .retrieve()
        .bodyToMono(CapabilityStatement.class)
        .doOnError(e -> logger.warn("Error while fetching metadata: {}", e.getMessage()));
  }

  public <T extends Resource<T>> Mono<T> fetchResource(Class<T> type, String id) {
    logger.debug("Fetch {} with id: {}", type.getSimpleName(), id);
    return client.get()
        .uri("/{type}/{id}", type.getSimpleName(), id)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> response.bodyToMono(type);
          case NOT_FOUND -> Mono.empty();
          default -> response.createException().flatMap(Mono::error);
        });
  }

  public Mono<Task> fetchTask(String id) {
    logger.debug("Fetch Task with id: {}", id);
    return client.get()
        .uri("/Task/{id}", id)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> response.bodyToMono(Task.class);
          case NOT_FOUND -> resourceNotFound(response, "Task", id);
          default -> response.createException().flatMap(Mono::error);
        });
  }

  public Flux<Task> listNewestTasks() {
    logger.debug("List newest Tasks");
    return client.get()
        .uri(uriBuilder -> uriBuilder.pathSegment("Task")
            .queryParam("_sort", "-_lastUpdated")
            .queryParam("_count", "50")
            .build())
        .exchangeToFlux(listHandler(Task.class));
  }

  /**
   * Lists Tasks with status {@link TaskStatus#READY ready} and the given {@code code}.
   *
   * @param code   the {@link Task#code() task code} to filter for
   * @param status the {@link Task#status() task status} to filter for
   * @return all Tasks which are currently in ready state and have the given {@code code}
   */
  public Flux<Task> listTasks(TaskCode code, Instant since, TaskStatus... status) {
    var statusQuery = Stream.of(status).map(TaskStatus::searchToken)
        .collect(Collectors.joining(","));
    logger.debug("List `{}` Tasks with code `{}` new since: {}", statusQuery, code.searchToken(),
        since);
    return client.get()
        .uri(uriBuilder -> uriBuilder.pathSegment("Task")
            .queryParam("status", statusQuery)
            .queryParam("code", code.searchToken())
            .queryParam("_lastUpdated", "ge" + since)
            .build())
        .exchangeToFlux(listHandler(Task.class))
        // TODO: use Retry.filter here to retry only certain errors
        .retryWhen(Retry.backoff(5, Duration.ofMillis(100)));
  }

  public Mono<Task> createTask(Task task) {
    logger.debug("Create Task");
    return client.post()
        .uri("/Task")
        .contentType(APPLICATION_JSON)
        .bodyValue(task)
        .retrieve()
        .bodyToMono(Task.class);
  }

  public Mono<Task> createBeamTask(Task task) {
    return task.findIdentifierValue(Util.BEAM_TASK_ID_SYSTEM).map(
        beamTaskId -> {
          logger.debug("Create Beam Task with id: {}", beamTaskId);
          return client.post()
              .uri("/Task")
              .contentType(APPLICATION_JSON)
              .header("If-None-Exist",
                  "identifier=%s|%s".formatted(Util.BEAM_TASK_ID_SYSTEM, beamTaskId))
              .bodyValue(task)
              .retrieve()
              .bodyToMono(Task.class);
        }
    ).orElse(Mono.error(new Exception("Missing Beam ID")));
  }

  /**
   * Updates {@code task}.
   *
   * @param task the Task to update
   * @return the updated Task
   * @throws NoSuchElementException if the task has no id
   */
  public Mono<Task> updateTask(Task task) {
    return Optionals.orElseGet(task.id(), task.meta().flatMap(Meta::versionId),
        (id, versionId) -> {
          logger.debug("Update Task with id `{}` and versionId `{}`", id, versionId);
          return client.put()
              .uri("/Task/{id}", id)
              .contentType(APPLICATION_JSON)
              .header("If-Match", "W/\"%s\"".formatted(versionId))
              .bodyValue(task)
              .retrieve()
              .bodyToMono(Task.class);
        },
        () -> Mono.error(new Exception("Missing Task.id.")),
        () -> Mono.error(new Exception("Missing Task.meta.versionId.")));
  }

  public Flux<Task> fetchTaskHistory(String id) {
    return client.get()
        .uri("/Task/{id}/_history", id)
        .retrieve()
        .bodyToMono(Bundle.class)
        .flatMapIterable(b -> b.resourcesAs(Task.class).toList());
  }

  public Flux<ActivityDefinition> listAllActivityDefinitions() {
    return listAll("/ActivityDefinition", ActivityDefinition.class);
  }

  public Mono<ActivityDefinition> fetchActivityDefinition(String id) {
    return client.get()
        .uri("/ActivityDefinition/{id}", id)
        .retrieve()
        .bodyToMono(ActivityDefinition.class);
  }

  public <T extends Resource<T>> Mono<T> findByUrl(Class<T> type, String url) {
    return client.get()
        .uri("/{type}?url={url}", type.getSimpleName(), url)
        .retrieve()
        .bodyToMono(Bundle.class)
        .flatMap(bundle -> Mono.justOrEmpty(bundle.resourcesAs(type).findFirst()));
  }

  public Mono<ActivityDefinition> createActivityDefinition(ActivityDefinition activityDefinition) {
    return activityDefinition.url().map(url -> client.post()
        .uri("/ActivityDefinition")
        .contentType(APPLICATION_JSON)
        .header("If-None-Exist", "url=%s".formatted(url))
        .bodyValue(activityDefinition)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK, CREATED -> response.bodyToMono(ActivityDefinition.class);
          case BAD_REQUEST -> badRequest(response, "Error while creating an ActivityDefinition");
          case NOT_FOUND -> notFound(response, "ActivityDefinition endpoint not found");
          default -> response.createException().flatMap(Mono::error);
        })
    ).orElse(Mono.error(new Exception("Missing ActivityDefinition URL")));
  }

  public Mono<MeasureReport> fetchMeasureReport(String id) {
    logger.debug("Fetch MeasureReport with id: {}", id);
    return client.get()
        .uri("/MeasureReport/{id}", id)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> response.bodyToMono(MeasureReport.class);
          case NOT_FOUND -> resourceNotFound(response, "MeasureReport", id);
          default -> response.createException().flatMap(Mono::error);
        });
  }

  public Mono<MeasureReport> createMeasureReport(MeasureReport measureReport) {
    return client.post()
        .uri("/MeasureReport")
        .contentType(APPLICATION_JSON)
        .bodyValue(measureReport)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case CREATED -> response.bodyToMono(MeasureReport.class);
          case BAD_REQUEST -> badRequest(response, "Error while creating a MeasureReport");
          case NOT_FOUND -> notFound(response, "MeasureReport endpoint not found");
          default -> response.createException().flatMap(Mono::error);
        });
  }

  private <T extends Resource<T>> Flux<T> listAll(String uri, Class<T> type) {
    return client.get().uri(uri).exchangeToFlux(listHandler(type));
  }

  private <T extends Resource<T>> Function<ClientResponse, Flux<T>> listHandler(Class<T> type) {
    return response -> switch (response.statusCode()) {
      case OK -> response.bodyToFlux(Bundle.class)
          .flatMapIterable(b -> b.resourcesAs(type).toList());
      case BAD_REQUEST -> TaskStore.<T>badRequest(response,
          "Error while listing %s".formatted(type.getSimpleName())).flux();
      case NOT_FOUND -> TaskStore.<T>notFound(response,
          "%s endpoint not found".formatted(type.getSimpleName())).flux();
      default -> response.createException().flatMap(Mono::<T>error).flux();
    };
  }

  private static <T> Mono<T> badRequest(ClientResponse response, String message) {
    logger.warn(message);
    if (response.headers().contentLength().orElse(0) > 0) {
      return response.bodyToMono(OperationOutcome.class)
          .flatMap(outcome -> Mono.error(new BadRequestException(message, outcome)));
    } else {
      return Mono.error(new BadRequestException(message));
    }
  }

  private static <T> Mono<T> notFound(ClientResponse response, String message) {
    logger.warn(message);
    if (response.headers().contentLength().orElse(0) > 0) {
      return response.bodyToMono(OperationOutcome.class)
          .flatMap(outcome -> Mono.error(new NotFoundException(message, outcome)));
    } else {
      return Mono.error(new NotFoundException(message));
    }
  }

  private static <T> Mono<T> resourceNotFound(ClientResponse response, String type, String id) {
    logger.warn("%s with id `%s` was not found.".formatted(type, id));
    return response.releaseBody().then(Mono.error(new ResourceNotFoundException(type, id)));
  }
}
