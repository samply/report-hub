package de.samply.reporthub.service;

import static de.samply.reporthub.Util.checkArgument;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.samply.reporthub.Util;
import de.samply.reporthub.component.BeamClient;
import de.samply.reporthub.model.beam.BeamTask;
import de.samply.reporthub.model.beam.BeamTask.FailureStrategy;
import de.samply.reporthub.model.beam.BeamTask.FailureStrategy.Retry;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.MessageHeader.Destination;
import de.samply.reporthub.model.fhir.Url;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BeamMessageBroker implements MessageBroker {

  private static final Logger logger = LoggerFactory.getLogger(BeamMessageBroker.class);

  public static final FailureStrategy DEFAULT_FAILURE_STRATEGY =
      new FailureStrategy(new Retry(1000, 5));

  private final String appId;
  private final BeamClient client;

  public BeamMessageBroker(@Value("${app.beam.appId}") String appId, BeamClient client) {
    this.appId = Objects.requireNonNull(appId);
    this.client = Objects.requireNonNull(client);
  }

  @Override
  public Mono<Void> send(Bundle message) {
    checkArgument(message.type().hasValue("message"), "Bundle type `message` expected.");
    var header = header(message);
    var id = header.id()
        .orElseThrow(() -> new IllegalArgumentException("Message header id expected."));
    logger.debug("Send message with id: {}", id);
    var to = to(header);
    var body = body(message);
    var task = task(UUID.fromString(id), to, body);
    return client.createTask(task)
        .doOnSuccess(x -> logger.debug("Successfully sent message with id: {}", id))
        .onErrorResume(e -> Mono.error(new Exception("Error while sending message `%s`: %s"
            .formatted(body, e.getMessage()))));
  }

  private static MessageHeader header(Bundle message) {
    return message.findFirstResource(MessageHeader.class)
        .orElseThrow(() -> new IllegalArgumentException("Message header expected"));
  }

  private static List<String> to(MessageHeader header) {
    return header.destination().stream().map(Destination::endpoint).map(Url::value).flatMap(
        Optional::stream).toList();
  }

  private BeamTask task(UUID id, List<String> to, String body) {
    return new BeamTask(id, appId, to, "foo", body, DEFAULT_FAILURE_STRATEGY);
  }

  private static String body(Bundle message) {
    try {
      return Util.mapper().writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Bundle could not be serialized to JSON.", e);
    }
  }

  @Override
  public Flux<Record> receive(Predicate<Bundle> messagePredicate) {
    return client.retrieveTasks()
        .flatMap(task -> parseBody(task).onErrorResume(e -> {
              logger.warn("Skip the task with id `{}` because of an error while parsing the body: {}",
                  task.id(), e.getMessage());
              return client.claimTask(task).then(Mono.empty());
            })
            .flatMap(message -> messagePredicate.test(message)
                ? Mono.just(message)
                : client.claimTask(task).then(Mono.empty()))
            .map(message -> Record.of(message, () -> {
              logger.debug("Acknowledge message with id: {}", message.id().orElseThrow());
              return client.claimTask(task);
            })));
  }

  private static Mono<Bundle> parseBody(BeamTask task) {
    return Util.parseJson(task.body(), Bundle.class);
  }
}
