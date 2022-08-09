package de.samply.reporthub.service;

import de.samply.reporthub.model.MessageEvent;
import de.samply.reporthub.model.TaskCode;
import de.samply.reporthub.model.TaskInput;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.Parameters;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.model.fhir.TaskStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EvaluateMeasureMessageService {

  private static final Logger logger = LoggerFactory.getLogger(EvaluateMeasureMessageService.class);

  public static final String GENERATE_DASHBOARD_REPORT_URL =
      "https://dktk.dkfz.de/fhir/ActivityDefinition/generate-exliquid-dashboard-report";

  private static final Predicate<Bundle> EVALUATE_MEASURE_MESSAGE = Bundle.hasFirstResource(
      MessageHeader.class, MessageHeader.hasEventCoding(MessageEvent.EVALUATE_MEASURE));

  private final MessageBroker messageBroker;
  private final TaskStore taskStore;
  private final Clock clock;

  private final Disposable.Swap subscription = Disposables.swap();

  public EvaluateMeasureMessageService(MessageBroker messageBroker, TaskStore taskStore,
      Clock clock) {
    this.messageBroker = Objects.requireNonNull(messageBroker);
    this.taskStore = Objects.requireNonNull(taskStore);
    this.clock = clock;
  }

  public boolean isRunning() {
    return !subscription.get().isDisposed();
  }

  @PostConstruct
  public void restart() {
    logger.info("(Re)Start processing messages.");
    subscription.update(pipeline().subscribe(EvaluateMeasureMessageService::logOutcome,
        EvaluateMeasureMessageService::logError));
  }

  public void stop() {
    logger.info("Stop processing messages.");
    subscription.update(Disposables.disposed());
  }

  Flux<Bundle> pipeline() {
    return messageBroker.receive(EVALUATE_MEASURE_MESSAGE)
        .flatMap(this::processRecord)
        .repeat();
  }

  Mono<Bundle> processRecord(Record record) {
    logger.debug("Process message with id: {}", record.message().id().orElseThrow());
    return task(record.message())
        .flatMap(task -> taskStore.createTask(task)
            .doOnNext(createdTask -> logger.debug("Created Task with id: {}", createdTask.id()
                .orElseThrow()))
            .flatMap(createdTask -> record.acknowledge().thenReturn(record.message())))
        .onErrorResume(e -> {
          logger.debug("Skip message because of: {}", e.getMessage());
          return record.acknowledge().then(Mono.empty());
        });
  }

  private Mono<Task> task(Bundle message) {
    return parameters(message)
        .flatMap(parameters -> parameters.findParameterValue(Canonical.class, "measure")
            .map(Mono::just)
            .orElseGet(() -> Mono.error(new Exception("Missing parameter with name `message` and "
                + "type canonical."))))
        .map(this::task);
  }

  private static Mono<Parameters> parameters(Bundle message) {
    return message.findFirstResource(MessageHeader.class)
        .flatMap(MessageHeader::findFirstFocus)
        .flatMap(focus -> message.resolveResource(Parameters.class, focus))
        .map(Mono::just)
        .orElseGet(() -> Mono.error(new Exception("Missing message parameter")));
  }

  private Task task(Canonical measure) {
    return Task.builder(TaskStatus.READY.code())
        .withInstantiatesCanonical(GENERATE_DASHBOARD_REPORT_URL)
        .withCode(CodeableConcept.of(TaskCode.EVALUATE_MEASURE.coding()))
        .withLastModified(OffsetDateTime.now(clock))
        .withInput(List.of(Input.of(TaskInput.MEASURE.coding(), measure)))
        .build();
  }

  private static void logOutcome(Bundle message) {
    logger.debug("Successfully processed message with id: {}", message.id().orElseThrow());
  }

  private static void logError(Throwable e) {
    logger.error(
        "Error while processing messages: {} Please restart the EvaluateMeasureMessageService.",
        e.getMessage());
  }
}
