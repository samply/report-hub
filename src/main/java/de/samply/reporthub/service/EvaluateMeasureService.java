package de.samply.reporthub.service;

import static de.samply.reporthub.model.TaskCode.EVALUATE_MEASURE;
import static de.samply.reporthub.model.TaskInput.MEASURE;
import static de.samply.reporthub.model.fhir.TaskStatus.COMPLETED;
import static de.samply.reporthub.model.fhir.TaskStatus.FAILED;
import static de.samply.reporthub.model.fhir.TaskStatus.IN_PROGRESS;

import de.samply.reporthub.model.TaskCode;
import de.samply.reporthub.model.TaskInput;
import de.samply.reporthub.model.TaskOutput;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.Meta;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Output;
import de.samply.reporthub.model.fhir.TaskStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
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
import reactor.retry.Repeat;

/**
 * Processes Tasks with {@link Task#code() code}
 * {@link TaskCode#EVALUATE_MEASURE evaluate-measure}.
 * <p>
 * The following will be done with each Task:
 * <ul>
 *   <li>update its status to {@link TaskStatus#IN_PROGRESS in-progress}</li>
 *   <li>evaluate the Measure with the URL taken from the input {@link TaskInput#MEASURE measure}</li>
 *   <li>store the resulting MeasureReport and reference it in the output {@link TaskOutput#MEASURE_REPORT measure-report}</li>
 *   <li>update its status to {@link TaskStatus#COMPLETED completed}</li>
 * </ul>
 * <p>
 * In case any error happens besides updating the Task itself, the task status is set to
 * {@link TaskStatus#FAILED failed} with the error message in the output {@link TaskOutput#ERROR error}.
 * <p>
 * In case that the task itself can't be updated, the processing is stopped
 */
@Service
public class EvaluateMeasureService {

  private static final Logger logger = LoggerFactory.getLogger(EvaluateMeasureService.class);

  private static final Predicate<CodeableConcept> MEASURE_CONCEPT =
      CodeableConcept.containsCoding(MEASURE);

  private final TaskStore taskStore;
  private final DataStore dataStore;
  private final Clock clock;

  private final Disposable.Swap subscription = Disposables.swap();

  public EvaluateMeasureService(TaskStore taskStore, DataStore dataStore, Clock clock) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.dataStore = Objects.requireNonNull(dataStore);
    this.clock = Objects.requireNonNull(clock);
  }

  public boolean isRunning() {
    return !subscription.get().isDisposed();
  }

  @PostConstruct
  public void restart() {
    logger.info("(Re)Start processing Tasks.");
    subscription.update(pipeline()
        .subscribe(EvaluateMeasureService::logOutcome, EvaluateMeasureService::logError));
  }

  public void stop() {
    logger.info("Stop processing Tasks.");
    subscription.update(Disposables.disposed());
  }

  Flux<Task> pipeline() {
    return taskStore.listReadyTasks(EVALUATE_MEASURE)
        .flatMap(this::processTask)
        .repeatWhen(Repeat.times(Long.MAX_VALUE).fixedBackoff(Duration.ofSeconds(1)));
  }

  /**
   * Processes the {@code readyTask}, updating it in the {@link TaskStore} as it goes and returns
   * either a {@link TaskStatus#COMPLETED completed} or a {@link TaskStatus#FAILED failed} Task
   * depending on the processing outcome or an {@link Mono#error(Throwable) error} if the Task could
   * not be updated in the {@link TaskStore}.
   *
   * @param readyTask the Task with status {@link TaskStatus#READY ready} to process
   * @return a {@link TaskStatus#COMPLETED completed} Task on success, a
   * {@link TaskStatus#FAILED failed} Task on error or an {@link Mono#error(Throwable) error} if the
   * Task could not be updated in the {@link TaskStore}
   */
  Mono<Task> processTask(Task readyTask) {
    return startProgress(readyTask)
        .flatMap(inProgressTask -> evaluateMeasure(inProgressTask)
            .onErrorResume(e -> fail(inProgressTask, e.getMessage())))
        .doOnError(e -> logger.error("Error while processing the Task with id `{}`: {}",
            readyTask.id().orElseThrow(), e.getMessage()));
  }

  private Mono<Task> startProgress(Task task) {
    logger.debug("Start processing the Task with id `{}` and versionId `{}`.",
        task.id().orElse("<unknown>"), task.meta().flatMap(Meta::versionId).orElse("<unknown>"));
    return taskStore.updateTask(task.withStatus(IN_PROGRESS.code())
        .withLastModified(OffsetDateTime.now(clock)));
  }

  private Mono<Task> evaluateMeasure(Task task) {
    return measureUrl(task)
        .flatMap(dataStore::evaluateMeasure)
        .flatMap(taskStore::createMeasureReport)
        .flatMap(measureReport -> complete(task, measureReport));
  }

  private Mono<String> measureUrl(Task task) {
    return task.findInput(MEASURE_CONCEPT)
        .flatMap(input -> input.castValue(Canonical.class))
        .flatMap(Canonical::value)
        .map(Mono::just)
        .orElseGet(() -> Mono.error(new Exception("Missing Measure URL in Task input.")));
  }

  private Mono<Task> complete(Task task, MeasureReport report) {
    logger.debug("Complete Task with id: {}", task.id().orElseThrow());
    return taskStore.updateTask(task.withStatus(COMPLETED.code())
        .addOutput(Output.of(TaskOutput.MEASURE_REPORT.coding(),
            Reference.ofReference("MeasureReport", report.id().orElseThrow())))
        .withLastModified(OffsetDateTime.now(clock)));
  }

  private Mono<Task> fail(Task task, String message) {
    logger.debug("Fail Task with id `{}` because of: {}", task.id().orElseThrow(), message);
    return taskStore.updateTask(task.withStatus(FAILED.code())
        .addOutput(Output.of(TaskOutput.ERROR.coding(), CodeableConcept.of(message)))
        .withLastModified(OffsetDateTime.now(clock)));
  }

  private static void logOutcome(Task task) {
    if (task.status().hasValue("completed")) {
      logger.debug("Successfully processed task with id: {}", task.id().orElseThrow());
    } else {
      logger.debug("Failed to process task with id `{}`: {}", task.id().orElseThrow(),
          task.findOutput(TaskOutput.ERROR.codeableConceptPredicate())
              .flatMap(o -> o.castValue(CodeableConcept.class))
              .flatMap(CodeableConcept::text)
              .orElseThrow());
    }
  }

  private static void logError(Throwable e) {
    logger.error("Error while processing Tasks: {} Please restart the EvaluateMeasureService.",
        e.getMessage());
  }
}
