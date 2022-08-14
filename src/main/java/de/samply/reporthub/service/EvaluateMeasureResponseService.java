package de.samply.reporthub.service;

import static de.samply.reporthub.dktk.model.fhir.TaskCode.EVALUATE_MEASURE;
import static de.samply.reporthub.model.fhir.TaskStatus.COMPLETED;
import static de.samply.reporthub.model.fhir.TaskStatus.FAILED;

import de.samply.reporthub.dktk.model.fhir.MessageEvent;
import de.samply.reporthub.dktk.model.fhir.TaskOutput;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Bundle.Entry;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.IssueSeverity;
import de.samply.reporthub.model.fhir.IssueType;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.MessageHeader.Destination;
import de.samply.reporthub.model.fhir.MessageHeader.Response;
import de.samply.reporthub.model.fhir.Meta;
import de.samply.reporthub.model.fhir.OperationOutcome;
import de.samply.reporthub.model.fhir.OperationOutcome.Issue;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.Resource;
import de.samply.reporthub.model.fhir.ResponseType;
import de.samply.reporthub.model.fhir.StringElement;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Uri;
import de.samply.reporthub.model.fhir.Url;
import de.samply.reporthub.service.fhir.messaging.MessageBroker;
import de.samply.reporthub.service.fhir.store.TaskStore;
import de.samply.reporthub.util.Monos;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EvaluateMeasureResponseService {

  private static final Logger logger = LoggerFactory.getLogger(
      EvaluateMeasureResponseService.class);

  public static final String MEASURE_ID_EXTENSION_URL =
      "https://dktk.dkfz.de/fhir/Extension/measure-id";

  public static final String MEASURE_DESTINATION_EXTENSION_URL =
      "https://dktk.dkfz.de/fhir/Extension/measure-destination";

  private final TaskStore taskStore;
  private final MessageBroker messageBroker;
  private final Clock clock;

  private final Disposable.Swap subscription = Disposables.swap();

  public EvaluateMeasureResponseService(TaskStore taskStore, MessageBroker messageBroker,
      Clock clock) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.messageBroker = Objects.requireNonNull(messageBroker);
    this.clock = Objects.requireNonNull(clock);
  }

  public boolean isRunning() {
    return !subscription.get().isDisposed();
  }

  @PostConstruct
  public void restart() {
    logger.info("(Re)Start processing tasks.");
    subscription.update(
        pipeline(clock.instant()).subscribe(EvaluateMeasureResponseService::logOutcome,
            EvaluateMeasureResponseService::logError));
  }

  public void stop() {
    logger.info("Stop processing tasks.");
    subscription.update(Disposables.disposed());
  }

  Flux<Instant> pipeline(Instant since) {
    return Mono.just(since).expand(this::poll);
  }

  Mono<Instant> poll(Instant since) {
    return taskStore.listTasks(EVALUATE_MEASURE, since, COMPLETED, FAILED)
        .flatMap(this::processTask)
        .collectList()
        .map(tasks -> tasks.stream()
            .map(Resource::meta).flatMap(Optional::stream)
            .map(Meta::lastUpdated).flatMap(Optional::stream)
            // TODO: fix date-time indexing in Blaze
            .sorted().findFirst().map(i -> i.plusSeconds(1)).orElse(since))
        .delayElement(Duration.ofSeconds(1));
  }

  Mono<Task> processTask(Task task) {
    return Monos.flatMap(measureId(task), destination(task), (messageId, destination) ->
            message(messageId, destination, task))
        .flatMap(messageBroker::send).thenReturn(task);
  }

  static Mono<String> measureId(Task task) {
    return Mono.justOrEmpty(task.findExtension(MEASURE_ID_EXTENSION_URL)
        .flatMap(e -> e.castValue(StringElement.class))
        .flatMap(StringElement::value));
  }

  static Mono<Url> destination(Task task) {
    return Mono.justOrEmpty(task.findExtension(MEASURE_DESTINATION_EXTENSION_URL)
        .flatMap(e -> e.castValue(Url.class)));
  }

  Mono<Bundle> message(String messageId, Url destination, Task task) {
    logger.debug("Process Task with id: {}", task.id().orElse("<unknown>"));
    return COMPLETED.test(task.status())
        ? okMessage(messageId, destination, task)
        : fatalErrorMessage(messageId, destination, task);
  }

  Mono<Bundle> okMessage(String messageId, Url destination, Task task) {
    return measureReport(task).map(measureReport -> message(Response.of(messageId,
        ResponseType.OK.code()), destination, measureReport));
  }

  Mono<Bundle> fatalErrorMessage(String messageId, Url destination, Task task) {
    return error(task).map(error -> message(Response.of(messageId,
        ResponseType.FATAL_ERROR.code()), destination, error));
  }

  Mono<MeasureReport> measureReport(Task task) {
    return task.findOutput(CodeableConcept.containsCoding(TaskOutput.MEASURE_REPORT))
        .flatMap(o -> o.castValue(Reference.class))
        .map(Mono::just)
        .orElseGet(() -> Mono.error(new Exception("Missing `%s` output in Task with id `%s`."
            .formatted(TaskOutput.MEASURE_REPORT.coding(), task.id().orElse("<unknown>")))))
        .flatMap(r -> r.resolve(taskStore, MeasureReport.class));
  }

  Mono<OperationOutcome> error(Task task) {
    return Monos.justOrError(task.findOutput(CodeableConcept.containsCoding(TaskOutput.ERROR))
                .flatMap(o -> o.castValue(StringElement.class))
                .flatMap(StringElement::value),
            () -> new Exception("Missing `%s` output in Task with id `%s`."
                .formatted(TaskOutput.ERROR.coding(), task.id().orElse("<unknown>"))))
        .map(error -> OperationOutcome.issue(Issue.builder(IssueSeverity.ERROR.code(),
            IssueType.PROCESSING.code()).withDiagnostics(error).build()));
  }

  static Bundle message(Response response, Url destination, Resource<?> focus) {
    var focusUrn = "urn:uuid:" + UUID.randomUUID();
    return Bundle.message()
        .withEntry(List.of(
            Entry.builder()
                .withResource(messageHeader(Destination.endpoint(destination), response, focusUrn))
                .build(),
            Entry.builder()
                .withFullUrl(Uri.valueOf(focusUrn))
                .withResource(focus)
                .build()
        ))
        .build();
  }

  static MessageHeader messageHeader(Destination destination, Response response, String focusUrn) {
    return MessageHeader.builder(MessageEvent.EVALUATE_MEASURE_RESPONSE.coding())
        .withId(UUID.randomUUID().toString())
        .withDestination(List.of(destination))
        .withResponse(response)
        .withFocus(List.of(Reference.builder().withReference(focusUrn).build()))
        .build();
  }

  private static void logOutcome(Instant since) {
    logger.debug("Successfully processed tasks since: {}", since);
  }

  private static void logError(Throwable e) {
    logger.error(
        "Error while processing tasks: {} Please restart the EvaluateMeasureResponseService.",
        e.getMessage());
  }
}
