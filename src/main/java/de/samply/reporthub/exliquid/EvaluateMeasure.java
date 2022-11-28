package de.samply.reporthub.exliquid;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.samply.reporthub.ClasspathIo;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Attachment;
import de.samply.reporthub.model.fhir.Base64Binary;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.Library;
import de.samply.reporthub.model.fhir.Measure;
import de.samply.reporthub.service.fhir.store.DataStore;
import de.samply.reporthub.service.fhir.store.TaskStore;
import de.samply.reporthub.util.Monos;
import java.time.Duration;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class EvaluateMeasure {

  private static final Logger logger = LoggerFactory.getLogger(EvaluateMeasure.class);

  private final TaskStore taskStore;
  private final DataStore dataStore;

  public EvaluateMeasure(TaskStore taskStore, DataStore dataStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.dataStore = Objects.requireNonNull(dataStore);
  }

  @PostConstruct
  public void init() {
    logger.info("Ensure TaskStore has ActivityDefinitions...");
    ClasspathIo.slurp("exliquid/ActivityDefinition-generate-dashboard-report.json")
        .flatMap(s -> Util.parseJson(s, ActivityDefinition.class))
        .flatMap(taskStore::createActivityDefinition)
        .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
        .subscribe(EvaluateMeasure::logCreateActivityDefinitionSuccess,
            EvaluateMeasure::logCreateActivityDefinitionError);

    logger.info("Ensure DataStore has Measures and Libraries...");
    Monos.flatMap(loadMeasure(), loadLibrary(), dataStore::createMeasureAndLibrary)
        .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
        .subscribe(EvaluateMeasure::logCreateMeasureAndLibrarySuccess,
            EvaluateMeasure::logCreateMeasureAndLibraryError);
  }

  private Mono<Measure> loadMeasure() {
    return ClasspathIo.slurp("exliquid/Measure-dashboard.json")
        .flatMap(s -> Util.parseJson(s, Measure.class));
  }

  private Mono<Library> loadLibrary() {
    return ClasspathIo.slurp("exliquid/Library-dashboard.json")
        .flatMap(s -> Util.parseJson(s, Library.class)
            .flatMap(library -> ClasspathIo.slurp("exliquid/Library-dashboard.cql")
                .map(EvaluateMeasure::createCqlAttachment)
                .map(library::addContent)));
  }

  private static Attachment createCqlAttachment(String content) {
    return Attachment.builder()
        .withContentType(Code.valueOf("text/cql"))
        .withData(Base64Binary.encoded(content.getBytes(UTF_8)))
        .build();
  }

  private static void logCreateActivityDefinitionSuccess(ActivityDefinition activityDefinition) {
    logger.info("Successfully ensured ActivityDefinition `%s` exists.".formatted(
        activityDefinition.url().orElse("<unknown>")));
  }

  private static void logCreateActivityDefinitionError(Throwable e) {
    logger.error(
        "Error while ensuring ActivityDefinition(s) exist: {} Please restart the ReportHub.",
        e.getMessage());
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

  private static void logCreateMeasureAndLibraryError(Throwable e) {
    logger.error(
        "Error while ensuring Library and Measure resources exist: {} Please restart the ReportHub.",
        e.getMessage());
  }
}
