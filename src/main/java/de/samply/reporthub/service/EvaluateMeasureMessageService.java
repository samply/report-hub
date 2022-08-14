package de.samply.reporthub.service;

import static de.samply.reporthub.service.EvaluateMeasureResponseService.MEASURE_DESTINATION_EXTENSION_URL;
import static de.samply.reporthub.service.EvaluateMeasureResponseService.MEASURE_ID_EXTENSION_URL;

import de.samply.reporthub.Util;
import de.samply.reporthub.dktk.model.fhir.MessageEvent;
import de.samply.reporthub.dktk.model.fhir.TaskCode;
import de.samply.reporthub.dktk.model.fhir.TaskInput;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.BundleType;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Element;
import de.samply.reporthub.model.fhir.Extension;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.MessageHeader.Source;
import de.samply.reporthub.model.fhir.Parameters;
import de.samply.reporthub.model.fhir.Resource;
import de.samply.reporthub.model.fhir.StringElement;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.model.fhir.Url;
import de.samply.reporthub.service.fhir.messaging.MessageBroker;
import de.samply.reporthub.service.fhir.messaging.Record;
import de.samply.reporthub.service.fhir.store.TaskStore;
import de.samply.reporthub.util.Monos;
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

/**
 * This service handles FHIR messages with {@link MessageHeader#eventCoding() event code}
 * {@link MessageEvent#EVALUATE_MEASURE evaluate-measure} and creates Tasks with
 * {@link Task#code() code} {@link TaskCode#EVALUATE_MEASURE evaluate-measure}.
 */
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
    var message = record.message();
    return task(message)
        .onErrorResume(e -> {
          logger.debug("Skip message because of: {}", e.getMessage());
          return record.acknowledge();
        })
        .flatMap(task -> taskStore.createTask(task)
            .doOnNext(EvaluateMeasureMessageService::logCreatedTask)
            .flatMap(createdTask -> record.acknowledge().thenReturn(message)));
  }

  Mono<Task> task(Bundle message) {
    return !BundleType.MESSAGE.test(message.type())
        ? Mono.error(new WrongBundleTypeException(BundleType.MESSAGE, message.type()))
        : header(message)
            .flatMap(header -> messageId(header)
                .flatMap(messageId -> source(header)
                    .flatMap(source -> measure(message)
                        .map(measure -> {
                          logger.debug("Process message with id: {}", messageId);
                          return task(messageId, source, measure);
                        }))));
  }

  private static Mono<MessageHeader> header(Bundle message) {
    return Monos.justOrError(message.firstResourceAs(MessageHeader.class),
        Util::missingMessageHeader);
  }

  private static Mono<String> messageId(MessageHeader header) {
    return Monos.justOrError(header.id(), Util::missingMessageId);
  }

  private Mono<Url> source(MessageHeader header) {
    return Monos.justOrError(header.source().stream().findFirst().map(Source::endpoint),
        Util::missingMessageSource);
  }

  private static Mono<Canonical> measure(Bundle message) {
    return parameters(message).flatMap(p -> findParameterValue(p, Canonical.class,
        "measure"));
  }

  private static Mono<Parameters> parameters(Bundle message) {
    return Monos.justOrError(message.firstResourceAs(MessageHeader.class)
            .flatMap(MessageHeader::findFirstFocus)
            .flatMap(focus -> message.resolveResource(Parameters.class, focus)),
        () -> new Exception("Parameters resource in focus expected."));
  }

  private static <T extends Element> Mono<T> findParameterValue(Parameters parameters,
      Class<T> type, String name) {
    return Monos.justOrError(parameters.findParameterValue(type, name),
        () -> new Exception("Missing parameter with name `%s` and type %s."
            .formatted(name, type.getSimpleName())));
  }

  private Task task(String messageId, Url source, Canonical measure) {
    return Task.ready()
        .withExtension(List.of(
            Extension.of(MEASURE_ID_EXTENSION_URL, StringElement.valueOf(messageId)),
            Extension.of(MEASURE_DESTINATION_EXTENSION_URL, source)
        ))
        .withInstantiatesCanonical(GENERATE_DASHBOARD_REPORT_URL)
        .withCode(CodeableConcept.coding(TaskCode.EVALUATE_MEASURE.coding()))
        .withLastModified(OffsetDateTime.now(clock))
        .withInput(List.of(Input.of(TaskInput.MEASURE.coding(), measure)))
        .build();
  }

  private static void logCreatedTask(Task task) {
    logger.debug("Created Task with id: {}", task.id().orElseThrow());
  }

  private static void logOutcome(Bundle message) {
    logger.debug("Successfully processed message with id: {}", message.firstResourceAs(
        MessageHeader.class).flatMap(Resource::id).orElse("<unknown>"));
  }

  private static void logError(Throwable e) {
    logger.error(
        "Error while processing messages: {} Please restart the EvaluateMeasureMessageService.",
        e.getMessage());
  }
}
