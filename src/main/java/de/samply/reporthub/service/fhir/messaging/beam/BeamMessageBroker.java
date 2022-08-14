package de.samply.reporthub.service.fhir.messaging.beam;

import static de.samply.reporthub.Util.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.beam.BeamResult;
import de.samply.reporthub.model.beam.BeamResult.Status;
import de.samply.reporthub.model.beam.BeamTask;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.BundleType;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.MessageHeader.Destination;
import de.samply.reporthub.model.fhir.MessageHeader.Response;
import de.samply.reporthub.model.fhir.MessageHeader.Source;
import de.samply.reporthub.model.fhir.Resource;
import de.samply.reporthub.model.fhir.ResponseType;
import de.samply.reporthub.model.fhir.Url;
import de.samply.reporthub.service.WrongBundleTypeException;
import de.samply.reporthub.service.beam.BeamTaskBroker;
import de.samply.reporthub.service.fhir.messaging.MessageBroker;
import de.samply.reporthub.service.fhir.messaging.Record;
import java.util.Base64;
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

/**
 * This class implements a {@link MessageBroker} on top of a {@link BeamTaskBroker}.
 * <p>
 * Sending a message will create a new Beam Task, while sending a response message will create a
 * Beam Result. Currently only Beam Tasks can be received as messages but not results.
 */
@Service
public class BeamMessageBroker implements MessageBroker {

  private static final Logger logger = LoggerFactory.getLogger(BeamMessageBroker.class);

  private final String appId;
  private final BeamTaskBroker client;

  /**
   * Creates a new {@code BeamMessageBroker}.
   *
   * @param appId  the Beam Id of the application running the {@code BeamMessageBroker}. Will be
   *               used in {@code from} fields of Beam Tasks
   * @param client the {@code BeamTaskBroker} to use to talk with the Beam Proxy
   */
  public BeamMessageBroker(@Value("${app.beam.appId}") String appId, BeamTaskBroker client) {
    this.appId = Objects.requireNonNull(appId);
    this.client = Objects.requireNonNull(client);
  }

  /**
   * Sends a FHIR Message.
   *
   * @param message the FHIR Message to send
   * @return a {@code Mono} that completes after the FHIR Message is sent
   * @throws IllegalArgumentException if the message Bundle doesn't have the type {@code message} or
   *                                  if the message Bundle doesn't have a header or if the header
   *                                  doesn't have a id
   */
  @Override
  public Mono<Void> send(Bundle message) {
    checkArgument(BundleType.MESSAGE.test(message.type()), "Bundle type `message` expected.");
    var header = header(message);
    var id = header.id().orElseThrow(Util::missingMessageId);
    logger.debug("Send message with id: {}", id);
    var to = to(header);
    return header.response().stream().findFirst()
        .map(response -> {
          var originalTaskId = UUID.fromString(response.identifier());
          return focus(message)
              .flatMap(focus -> Util.printJson(focus)
                  .flatMap(
                      body -> client.answerTask(BeamResult.base64Body(appId, to, originalTaskId,
                          status(response), body))));
        })
        .orElseGet(() -> Util.printJson(message).map(BeamMessageBroker::base64Encode)
            .flatMap(body -> client.createTask(task(UUID.fromString(id), to, body))
                .doOnSuccess(x -> logger.debug("Successfully sent message with id: {}", id))
                .onErrorResume(e -> Mono.error(
                    new Exception("Error while sending message `%s`: %s"
                        .formatted(body, e.getMessage()))))));
  }

  private static Mono<? extends Resource<?>> focus(Bundle message) {
    return message.firstResourceAs(MessageHeader.class)
        .flatMap(MessageHeader::findFirstFocus)
        .flatMap(message::resolveResource)
        .map(Mono::just)
        .orElseGet(() -> Mono.error(new Exception("Missing resource in focus.")));
  }

  private static String base64Encode(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes(UTF_8));
  }

  private Status status(Response response) {
    return ResponseType.OK.test(response.code()) ? Status.SUCCEEDED : Status.PERM_FAILED;
  }

  private static MessageHeader header(Bundle message) {
    return message.firstResourceAs(MessageHeader.class)
        .orElseThrow(() -> new IllegalArgumentException("Message header expected"));
  }

  private static List<String> to(MessageHeader header) {
    return header.destination().stream().map(Destination::endpoint).map(Url::value).flatMap(
        Optional::stream).toList();
  }

  private BeamTask task(UUID id, List<String> to, String body) {
    return BeamTask.of(id, appId, to, body);
  }

  @Override
  public Flux<Record> receive(Predicate<Bundle> messagePredicate) {
    return client.retrieveTasks()
        .doOnNext(task -> logger.debug("Received Task with id: {}", task.id()))
        .flatMap(task -> parseBody(task)
            .flatMap(BeamMessageBroker::validateMessage)
            .flatMap(message -> addIdAndFromToHeader(message, task))
            .onErrorResume(e -> {
              logger.warn("Skip the task with id `{}` because of an error while parsing and "
                          + "validating the body: {}", task.id(), e.getMessage());
              return client.claimTask(task);
            })
            .doOnNext(message -> logger.debug("Converted task into message with id: {}", message
                .firstResourceAs(MessageHeader.class).flatMap(Resource::id).orElse("<unknown>")))
            .flatMap(message -> {
              if (messagePredicate.test(message)) {
                return Mono.just(message);
              } else {
                logger.debug("Skip the task with id `{}` because it didn't match the predicate: {}",
                    task.id(), messagePredicate);
                return client.claimTask(task);
              }
            })
            .map(message -> Record.of(message, () -> {
              logger.debug("Acknowledge message with id: {}", message.firstResourceAs(
                  MessageHeader.class).flatMap(Resource::id).orElse("<unknown>"));
              return client.claimTask(task);
            })));
  }

  Mono<Bundle> parseBody(BeamTask task) {
    return base64Decode(task.body()).flatMap(body -> Util.parseJson(body, Bundle.class));
  }

  static Mono<Bundle> validateMessage(Bundle message) {
    return BundleType.MESSAGE.test(message.type())
        ? message.firstResourceAs(MessageHeader.class).isPresent()
        ? Mono.just(message)
        : Mono.error(new Exception("Missing message header."))
        : Mono.error(new WrongBundleTypeException(BundleType.MESSAGE, message.type()));
  }

  private static Mono<Bundle> addIdAndFromToHeader(Bundle message, BeamTask task) {
    return message.mapFirstResource(MessageHeader.class,
            header -> header
                .withId(task.id().toString())
                .withSource(List.of(Source.endpoint(Url.valueOf(task.from())))))
        .map(Mono::just)
        .orElseGet(() -> Mono.error(new Exception("Missing header in message.")));
  }

  static Mono<String> base64Decode(String s) {
    try {
      return Mono.just(new String(Base64.getDecoder().decode(s), UTF_8));
    } catch (IllegalArgumentException e) {
      return Mono.error(new Exception("Error while decoding base64 encoded task body: " +
                                      e.getMessage(), e));
    }
  }
}
