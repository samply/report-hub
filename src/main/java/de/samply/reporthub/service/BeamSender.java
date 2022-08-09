package de.samply.reporthub.service;

import static de.samply.reporthub.Util.BEAM_TASK_ID_SYSTEM;
import static de.samply.reporthub.model.MessageEvent.FULFILL_TASK;

import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Bundle.Entry;
import de.samply.reporthub.model.fhir.BundleType;
import de.samply.reporthub.model.fhir.Endpoint;
import de.samply.reporthub.model.fhir.Identifier;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.MessageHeader.Destination;
import de.samply.reporthub.model.fhir.Organization;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Restriction;
import de.samply.reporthub.model.fhir.TaskStatus;
import de.samply.reporthub.model.fhir.Url;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Repeat;

/**
 * Sends a FHIR Message for every {@link TaskStatus#REQUESTED requested} Task with a single
 * {@link Restriction#recipient() recipient} to that recipient Beam app.
 */
//@Service
public class BeamSender {

  private static final Logger logger = LoggerFactory.getLogger(BeamSender.class);

  private final MessageBroker messageBroker;
  private final TaskStore taskStore;

  private final Disposable.Swap subscription = Disposables.swap();

  public BeamSender(MessageBroker messageBroker, TaskStore taskStore) {
    this.messageBroker = Objects.requireNonNull(messageBroker);
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  private static Optional<UUID> asUUID(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public boolean isRunning() {
    return !subscription.get().isDisposed();
  }

  @PostConstruct
  public void restart() {
    logger.info("(Re)Start processing Tasks.");
    subscription.update(pipeline()
        .subscribe(BeamSender::logOutcome, BeamSender::logError));
  }

  public void stop() {
    logger.info("Stop processing Tasks.");
    subscription.update(Disposables.disposed());
  }

  Flux<Task> pipeline() {
    return taskStore.listRequestedTasks()
        .flatMap(this::processTask)
        .repeatWhen(Repeat.times(Long.MAX_VALUE).fixedBackoff(Duration.ofSeconds(1)));
  }

  Mono<Task> processTask(Task requestedTask) {
    var taskId = requestedTask.id()
        .orElseThrow(() -> new IllegalArgumentException("Task id expected."));
    return singleRecipientAppId(requestedTask)
        .flatMap(appId -> ensureBeamTaskId(requestedTask)
            .flatMap(beamTaskId -> messageBroker.send(message(beamTaskId, appId, requestedTask))
                .doOnSuccess(sent -> logger.debug(
                    "Successfully sent message to fulfill the task with id `{}` to: {}", taskId,
                    appId))).doOnError(e -> logger.error(
                "Error while sending message to fulfill the requested Task with id `{}`: {}",
                taskId, e.getMessage()))
            .map(v -> requestedTask));
  }

  private Mono<UUID> ensureBeamTaskId(Task task) {
    return task.findIdentifierValue(BEAM_TASK_ID_SYSTEM)
        .flatMap(BeamSender::asUUID)
        .map(Mono::just)
        .orElse(setBeamTaskId(task, UUID.randomUUID()));
  }

  private Mono<UUID> setBeamTaskId(Task task, UUID beamTaskId) {
    return taskStore.updateTask(task.addIdentifier(Identifier.of(BEAM_TASK_ID_SYSTEM,
        beamTaskId.toString()))).map(t -> beamTaskId);
  }

  Mono<String> singleRecipientAppId(Task task) {
    var recipients = task.restriction().map(Restriction::recipient).stream()
        .flatMap(List::stream).toList();

    return switch (recipients.size()) {
      case 0 -> {
        logger.info("Skip task with id `{}` because it has no recipient.", task.id().orElseThrow());
        yield Mono.empty();
      }
      case 1 -> recipients.get(0).resolve(taskStore, Organization.class)
          .flatMap(organization -> Flux.fromIterable(organization.endpoint())
              .flatMap(ref -> ref.resolve(taskStore, Endpoint.class))
              .map(Endpoint::address)
              .next());
      default -> {
        logger.info("Skip task with id `{}` because it has more than one recipient.",
            task.id().orElseThrow());
        yield Mono.empty();
      }
    };
  }

  private static Bundle message(UUID beamTaskId, String appId, Task requestedTask) {
    return Bundle.builder(BundleType.MESSAGE.code())
        .withEntry(List.of(
            Entry.builder().withResource(messageHeader(beamTaskId, appId)).build(),
            Entry.builder().withResource(requestedTask).build()
        ))
        .build();
  }

  private static MessageHeader messageHeader(UUID beamTaskId, String appId) {
    return MessageHeader.builder(FULFILL_TASK.coding())
        .withId(beamTaskId.toString())
        .withDestination(List.of(new Destination(Url.valueOf(appId))))
        .build();
  }

  private static void logOutcome(Task task) {
    logger.debug("Successfully processed task with id: {}", task.id().orElseThrow());
  }

  private static void logError(Throwable e) {
    logger.error("Error while processing Tasks: {} Please restart the BeamSender.",
        e.getMessage());
  }
}
