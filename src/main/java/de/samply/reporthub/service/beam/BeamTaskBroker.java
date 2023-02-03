package de.samply.reporthub.service.beam;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.beam.BeamResult;
import de.samply.reporthub.model.beam.BeamTask;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This class represents a Beam Task Broker. It's possible to:
 * <ul>
 *   <li>{@link #retrieveTasks() retrieve tasks}</li>
 *   <li>{@link #createTask(BeamTask) create a task}</li>
 *   <li>{@link #claimTask(BeamTask) claim a task}</li>
 *   <li>{@link #answerTask(BeamResult) answer a task}</li>
 * </ul>
 */
@Service
public class BeamTaskBroker {

  private static final Logger logger = LoggerFactory.getLogger(BeamTaskBroker.class);

  private final WebClient client;
  private final String appId;

  public BeamTaskBroker(@Qualifier("beamProxy") WebClient client,
      @Value("${app.beam.appId}") String appId) {
    this.client = Objects.requireNonNull(client);
    this.appId = Objects.requireNonNull(appId);
  }

  public Flux<BeamTask> retrieveTasks() {
    logger.debug("Retrieve tasks");
    return client.get()
        .uri(builder -> builder.path("/v1/tasks")
            .queryParam("to", appId)
            .queryParam("filter", "todo")
            .queryParam("wait_count", "1")
            .queryParam("wait_time", "10000")
            .build())
        .exchangeToFlux(response -> switch (response.statusCode().value()) {
          case 200, 206 -> response.bodyToFlux(BeamTask.class);
          default -> response.createException().flatMap(Mono::<BeamTask>error).flux();
        });
  }

  public Mono<Void> createTask(BeamTask task) {
    logger.debug("Create task with id: {}", task.id());
    return client.post()
        .uri("/v1/tasks")
        .contentType(APPLICATION_JSON)
        .bodyValue(task)
        .exchangeToMono(response -> switch (response.statusCode().value()) {
          case 201, 409 -> response.releaseBody();
          case 400 -> response.bodyToMono(String.class)
              .flatMap(msg -> Mono.error(new Exception("Error while creating task `%s`: %s"
                  .formatted(json(task), msg))));
          default -> response.createException().flatMap(Mono::error);
        })
        .doOnSuccess(created -> logger.debug("Successfully created task with id: {}", task.id()))
        .doOnError(e -> logger.error("Error while creating task with id `{}`: {}", task.id(),
            e.getMessage()));
  }

  public <T> Mono<T> claimTask(BeamTask task) {
    logger.debug("Claim task with id: {}", task.id());
    return client.put()
        .uri("/v1/tasks/{taskId}/results/{appId}", task.id(), appId)
        .contentType(APPLICATION_JSON)
        .bodyValue(BeamResult.claimed(appId, List.of(task.from()), task.id()))
        .exchangeToMono(response -> switch (response.statusCode().value()) {
          case 201, 204 -> response.releaseBody().then(Mono.empty());
          case 400 -> response.bodyToMono(String.class).flatMap(msg -> {
            logger.error("Error while claiming the task with id `{}`: {}", task.id(), msg);
            return Mono.error(new Exception(msg));
          });
          default -> response.createException().flatMap(Mono::error);
        });
  }

  public Mono<Void> answerTask(BeamResult result) {
    logger.debug("Answer task with id: {}", result.task());
    return client.put()
        .uri("/v1/tasks/{taskId}/results/{appId}", result.task(), appId)
        .contentType(APPLICATION_JSON)
        .bodyValue(result)
        .exchangeToMono(response -> switch (response.statusCode().value()) {
          case 200, 204 -> response.releaseBody();
          case 400 -> response.bodyToMono(String.class).flatMap(msg -> {
            logger.error("Error while answering the task with id `{}`: {}", result.task(), msg);
            return Mono.error(new Exception(msg));
          });
          default -> response.createException().flatMap(Mono::error);
        });
  }

  private static String json(BeamTask task) {
    try {
      return Util.mapper().writeValueAsString(task);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Task could not be serialized to JSON.", e);
    }
  }
}
