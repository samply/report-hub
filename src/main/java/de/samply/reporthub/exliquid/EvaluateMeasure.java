package de.samply.reporthub.exliquid;

import static de.samply.reporthub.model.fhir.TaskStatus.ACCEPTED;
import static de.samply.reporthub.model.fhir.TaskStatus.COMPLETED;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.reporthub.ClasspathIo;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Attachment;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Coding;
import de.samply.reporthub.model.fhir.Library;
import de.samply.reporthub.model.fhir.Measure;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Output;
import de.samply.reporthub.service.BadRequestException;
import de.samply.reporthub.service.DataStore;
import de.samply.reporthub.service.TaskStore;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class EvaluateMeasure {

  private static final Logger logger = LoggerFactory.getLogger(EvaluateMeasure.class);

  public static final String GENERATE_DASHBOARD_REPORT_URL =
      "https://dktk.dkfz.de/fhir/ActivityDefinition/exliquid-generate-dashboard-report";
  public static final String MEASURE_EXLIQUID_URL = "https://dktk.dkfz.de/fhir/Measure/exliquid";

  private final TaskStore taskStore;
  private final DataStore dataStore;
  private final Clock clock;

  public EvaluateMeasure(TaskStore taskStore, DataStore dataStore, Clock clock) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.dataStore = Objects.requireNonNull(dataStore);
    this.clock = Objects.requireNonNull(clock);
  }

  @PostConstruct
  public void init() {
    logger.info("Ensure TaskStore has ActivityDefinitions...");
    ClasspathIo.slurp("exliquid/ActivityDefinition-generate-dashboard-export.json")
        .flatMap(s -> Util.parseJson(s, ActivityDefinition.class))
        .flatMap(taskStore::createActivityDefinition)
        .subscribe(EvaluateMeasure::logCreateActivityDefinitionSuccess, EvaluateMeasure::logError);

    logger.info("Ensure DataStore has Measures and Libraries...");
    loadMeasure()
        .flatMap(measure -> loadLibrary()
            .flatMap(library -> dataStore.createMeasureAndLibrary(measure, library)))
        .subscribe(EvaluateMeasure::logCreateMeasureAndLibrarySuccess, EvaluateMeasure::logError);

    logger.info("Subscribe to requested tasks.");
    taskStore.requestedTasks(GENERATE_DASHBOARD_REPORT_URL)
        .doOnNext(EvaluateMeasure::logReceivedTask)
        .flatMap(this::acceptTask)
        .flatMap(task -> dataStore.evaluateMeasure(MEASURE_EXLIQUID_URL)
            .flatMap(taskStore::createMeasureReport)
            .flatMap(measureReport -> completeTask(task, measureReport)))
        .subscribe(System.out::println, EvaluateMeasure::logError);
  }

  private Mono<Measure> loadMeasure() {
    return ClasspathIo.slurp("exliquid/Measure-exliquid.json")
        .flatMap(s -> Util.parseJson(s, Measure.class));
  }

  private Mono<Library> loadLibrary() {
    return ClasspathIo.slurp("exliquid/Library-exliquid.json")
        .flatMap(s -> Util.parseJson(s, Library.class)
            .flatMap(library -> ClasspathIo.slurp("exliquid/Library-exliquid.cql")
                .map(EvaluateMeasure::createCqlAttachment)
                .map(library::addContent)));
  }

  private static Attachment createCqlAttachment(String content) {
    return Attachment.builder()
        .withContentType(Code.valueOf("text/cql"))
        .withData(Base64.getEncoder().encodeToString(content.getBytes(UTF_8)))
        .build();
  }

  private Mono<Task> acceptTask(Task task) {
    return taskStore.updateTask(task.withStatus(ACCEPTED.code())
        .withLastModified(OffsetDateTime.now(clock)));
  }

  private Mono<Task> completeTask(Task task, MeasureReport measureReport) {
    return measureReportOutput(measureReport)
        .flatMap(output -> taskStore.updateTask(task.withStatus(COMPLETED.code())
            .withLastModified(OffsetDateTime.now(clock))
            .addOutput(output)));
  }

  private Mono<Output> measureReportOutput(MeasureReport measureReport) {
    return measureReport.id().map(id -> Output.builder(
            CodeableConcept.builder()
                .withCoding(List.of(
                    Coding.builder()
                        .withSystem("https://dktk.dkfz.de/fhir/CodeSystem/exliquid-task-output")
                        .withCode(Code.valueOf("measure-report"))
                        .build()))
                .build(),
            Reference.builder().withReference("MeasureReport/" + id).build()).build())
        .map(Mono::just)
        .orElse(Mono.error(new Exception("Missing MeasureReport ID.")));
  }

  private static void logCreateActivityDefinitionSuccess(ActivityDefinition activityDefinition) {
    logger.info("Successfully ensured ActivityDefinition `%s` exists.".formatted(
        activityDefinition.url().orElse("<unknown>")));
  }

  private static void logCreateMeasureAndLibrarySuccess(Bundle bundle) {
    bundle.resourcesAs(Measure.class).findFirst().ifPresentOrElse(measure ->
            logger.info("Successfully ensured Measure `%s` exists.".formatted(measure.url()
                .orElse("<unknown>"))),
        () -> logger.warn(
            "Missing Measure in result bundle of successful Measure and Library creation."));

    bundle.resourcesAs(Library.class).findFirst().ifPresentOrElse(library ->
            logger.info("Successfully ensured Library `%s` exists.".formatted(library.url()
                .orElse("<unknown>"))),
        () -> logger.warn(
            "Missing Library in result bundle of successful Measure and Library creation."));
  }

  private static void logError(Throwable e) {
    if (e instanceof WebClientResponseException) {
      logger.error(e.getMessage());
      logOperationOutcome((WebClientResponseException) e);
    } else if (e instanceof BadRequestException) {
      Util.prettyPrintJson(((BadRequestException) e).getOperationOutcome()).subscribe(
          outcome -> logger.error(e.getMessage() + ": {}", outcome));
    } else {
      logger.error(e.getMessage());
    }
  }

  private static void logOperationOutcome(WebClientResponseException e) {
    Util.operationOutcome(e).flatMap(Util::prettyPrintJson).subscribe(logger::error);
  }

  private static void logReceivedTask(Task task) {
    logger.debug("Received task with id `%s`.".formatted(task.id().orElse("<unknown>")));
  }
}
