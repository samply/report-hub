package de.samply.reporthub.service;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.CapabilityStatement;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.OperationOutcome;
import de.samply.reporthub.model.fhir.Resource;
import de.samply.reporthub.model.fhir.Task;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Repeat;

@Service
public class TaskStore implements Store {

  private static final Logger logger = LoggerFactory.getLogger(TaskStore.class);

  public static final String BEAM_TASK_ID_SYSTEM = "https://beam.samply.de/fhir/NamingSysten/taskId";

  private final WebClient client;

  private Flux<Task> requestedTasks;

  public TaskStore(@Qualifier("taskStoreClient") WebClient client) {
    this.client = Objects.requireNonNull(client);
  }

  @PostConstruct
  public void init() {
    requestedTasks = listAll("/Task?status=requested", Task.class)
        .repeatWhen(Repeat.times(Long.MAX_VALUE).fixedBackoff(Duration.ofSeconds(1)))
        .share();
  }

  public Mono<CapabilityStatement> fetchMetadata() {
    logger.debug("Fetching metadata...");
    return client.get()
        .uri("/metadata")
        .retrieve()
        .bodyToMono(CapabilityStatement.class)
        .doOnError(e -> logger.warn("Error while fetching metadata: {}", e.getMessage()));
  }

  public Mono<Task> fetchTask(String id) {
    return client.get()
        .uri("/Task/{id}", id)
        .retrieve()
        .bodyToMono(Task.class);
  }

  public Flux<Task> listAllTasks() {
    return listAll("/Task", Task.class);
  }

  public Flux<Task> requestedTasks(String canonical) {
    return requestedTasks.filter(task ->
        Optional.of(canonical).equals(task.instantiatesCanonical()));
  }

  public Mono<Task> createTask(Task task) {
    return client.post()
        .uri("/Task")
        .contentType(APPLICATION_JSON)
        .bodyValue(task)
        .retrieve()
        .bodyToMono(Task.class);
  }

  public Mono<Task> createBeamTask(Task task) {
    return task.findIdentifierValue(BEAM_TASK_ID_SYSTEM).map(
        beamTaskId -> client.post()
            .uri("/Task")
            .contentType(APPLICATION_JSON)
            .header("If-None-Exist", "identifier=%s|%s".formatted(BEAM_TASK_ID_SYSTEM, beamTaskId))
            .bodyValue(task)
            .retrieve()
            .bodyToMono(Task.class)
    ).orElse(Mono.error(new Exception("Missing Beam ID")));
  }

  /**
   * Updates {@code task}.
   *
   * @param task the Task to update
   * @return the updated Task
   */
  public Mono<Task> updateTask(Task task) {
    return task.id().map(
        id -> client.put()
            .uri("/Task/{id}", id)
            .contentType(APPLICATION_JSON)
            .bodyValue(task)
            .retrieve()
            .bodyToMono(Task.class)
    ).orElse(Mono.error(new Exception("Missing Task ID")));
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

  public Mono<ActivityDefinition> findActivityDefinitionByUrl(String url) {
    return client.get()
        .uri("/ActivityDefinition?url={url}", url)
        .retrieve()
        .bodyToMono(Bundle.class)
        .flatMap(bundle -> Mono.justOrEmpty(bundle.resourcesAs(ActivityDefinition.class)
            .findFirst()));
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
          default -> response.createException().flatMap(Mono::error);
        })
    ).orElse(Mono.error(new Exception("Missing ActivityDefinition URL")));
  }

  public Mono<MeasureReport> fetchMeasureReport(String id) {
    return client.get()
        .uri("/MeasureReport/{id}", id)
        .retrieve()
        .bodyToMono(MeasureReport.class);
  }

  public Mono<MeasureReport> createMeasureReport(MeasureReport measureReport) {
    return client.post()
        .uri("/MeasureReport")
        .contentType(APPLICATION_JSON)
        .bodyValue(measureReport)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case CREATED -> response.bodyToMono(MeasureReport.class);
          case BAD_REQUEST -> badRequest(response, "Error while creating a MeasureReport");
          default -> response.createException().flatMap(Mono::error);
        });
  }

  private <T extends Resource> Flux<T> listAll(String uri, Class<T> type) {
    return client.get()
        .uri(uri)
        .retrieve()
        .bodyToFlux(Bundle.class)
        .flatMapIterable(b -> b.resourcesAs(type).toList());
  }

  private <T> Mono<T> badRequest(ClientResponse response, String message) {
    return response.bodyToMono(OperationOutcome.class)
        .flatMap(outcome -> Mono.error(new BadRequestException(message + ": 400 Bad Request",
            outcome)));
  }
}
