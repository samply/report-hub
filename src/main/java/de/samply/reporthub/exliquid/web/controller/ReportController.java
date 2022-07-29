package de.samply.reporthub.exliquid.web.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import de.samply.reporthub.exliquid.web.model.Report;
import de.samply.reporthub.exliquid.web.model.Report.Stratum;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.MeasureReport.Group;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Stratifier;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Stratifier.Stratum.Component;
import de.samply.reporthub.service.ResourceNotFoundException;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.util.IntPair;
import de.samply.reporthub.util.Optionals;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@org.springframework.stereotype.Component("exliquidReportController")
public class ReportController {

  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

  public static final String MEASURE_POPULATION =
      "http://terminology.hl7.org/CodeSystem/measure-population";
  public static final String EXLIQUID_MEASURE_GROUP =
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-measure-group";
  public static final String EXLIQUID_STRATIFIER =
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-stratifier";

  public static final Predicate<CodeableConcept> PATIENT_GROUP_PREDICATE =
      CodeableConcept.containsCoding(EXLIQUID_MEASURE_GROUP, "patient");
  public static final Predicate<CodeableConcept> SPECIMEN_GROUP_PREDICATE =
      CodeableConcept.containsCoding(EXLIQUID_MEASURE_GROUP, "specimen");
  public static final Predicate<CodeableConcept> INITIAL_POPULATION_PREDICATE =
      CodeableConcept.containsCoding(MEASURE_POPULATION, "initial-population");
  public static final Predicate<CodeableConcept> DIAGNOSIS_STRATIFIER_PREDICATE =
      CodeableConcept.containsCoding(EXLIQUID_STRATIFIER, "diagnosis");
  public static final Predicate<CodeableConcept> SAMPLE_DIAGNOSIS_STRATIFIER_PREDICATE =
      CodeableConcept.containsCoding(EXLIQUID_STRATIFIER, "sample-diagnosis");
  public static final Predicate<CodeableConcept> SAMPLE_TYPE_STRATIFIER_PREDICATE =
      CodeableConcept.containsCoding(EXLIQUID_STRATIFIER, "sample-type");
  public static final String BLOOD_PLASMA = "blood-plasma";
  public static final String PERIPHERAL_BLOOD_CELLS_VITAL = "peripheral-blood-cells-vital";

  private final TaskStore taskStore;

  public ReportController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  @Bean
  public RouterFunction<ServerResponse> exliquidReportRouter() {
    return route(GET("exliquid-report/{id}"), this::handler);
  }

  Mono<ServerResponse> handler(ServerRequest request) {
    String id = request.pathVariable("id");
    logger.debug("Request EXLIQUID report with id: {}", id);
    return report(id)
        .flatMap(report -> ok().render("exliquid/report", Map.of("report", report)))
        .onErrorResume(ResourceNotFoundException.class, ReportController::notFound);
  }

  private static Mono<ServerResponse> notFound(ResourceNotFoundException e) {
    var error = "The EXLIQUID report with id `%s` was not found.".formatted(e.getId());
    logger.warn(error);
    return ok().render("404", Map.of("error", error));
  }

  Mono<Report> report(String id) {
    return taskStore.fetchMeasureReport(id)
        .map(ReportController::convert)
        .flatMap(Mono::justOrEmpty);
  }

  /**
   * Converts a FHIR {@link MeasureReport} {@code report} into {@link Report} used for EXLIQUID.
   *
   * @param report the FHIR {@code MeasureReport} to convert
   * @return an {@code Optional} of the converted {@link Report} or an empty {@code Optional} if the
   * conversion was not possible
   */
  static Optional<Report> convert(MeasureReport report) {
    return Optionals.flatMap(report.date(),
        report.findGroup(PATIENT_GROUP_PREDICATE),
        report.findGroup(SPECIMEN_GROUP_PREDICATE),
        ReportController::convertFrom);
  }

  private static Optional<Report> convertFrom(OffsetDateTime date, Group patientGroup,
      Group specimenGroup) {
    return Optionals.flatMap(patientGroup.findPopulation(INITIAL_POPULATION_PREDICATE),
        specimenGroup.findPopulation(INITIAL_POPULATION_PREDICATE),
        patientGroup.findStratifier(DIAGNOSIS_STRATIFIER_PREDICATE),
        specimenGroup.findStratifier(SAMPLE_DIAGNOSIS_STRATIFIER_PREDICATE),
        (patientPopulation, specimenPopulation, diagnosisStratifier, sampleDiagnosisStratifier) ->
            Optionals.map(patientPopulation.count(),
                specimenPopulation.count(),
                (totalNumberOfPatients, totalNumberOfSpecimen) ->
                    new Report(date,
                        totalNumberOfPatients,
                        totalNumberOfSpecimen,
                        strata(diagnosisStratifier, sampleDiagnosisStratifier))));
  }

  /**
   * Joins both stratifier by diagnosis and returns a list of combined {@link Stratum}.
   *
   * @param patientDiagnosisStratifier the stratifier from diagnosis to patient count
   * @param sampleDiagnosisStratifier  the stratifier from diagnosis-sample-type pairs to sample
   *                                   type
   * @return a list of combined {@link Stratum}
   */
  private static List<Stratum> strata(Stratifier patientDiagnosisStratifier,
      Stratifier sampleDiagnosisStratifier) {
    var diagnosisToPlasmaPbmcCounts = diagnosisToPlasmaPbmcCounts(sampleDiagnosisStratifier);
    return patientDiagnosisStratifier.stratum().stream()
        .flatMap(stratum -> stratum(diagnosisToPlasmaPbmcCounts, stratum).stream())
        .toList();
  }

  /**
   * Takes a patient-diagnosis stratum and a map from diagnosis to a {@link IntPair pair} of plasma
   * and PBMC counts and returns a stratum with patient, plasma and PBMC counts.
   *
   * @param diagnosisToPlasmaPbmcCounts a map from diagnosis to a {@link IntPair pair} of plasma and
   *                                    PBMC counts
   * @param stratum                     the stratum to process
   * @return a stratum with patient, plasma and PBMC counts
   */
  private static Optional<Stratum> stratum(
      Map<String, IntPair> diagnosisToPlasmaPbmcCounts,
      Stratifier.Stratum stratum) {
    return Optionals.flatMap(stratum.value().flatMap(CodeableConcept::text),
        stratum.findPopulation(INITIAL_POPULATION_PREDICATE)
            .flatMap(Stratifier.Stratum.Population::count),
        (diagnosis, patientCount) -> Optional.ofNullable(diagnosisToPlasmaPbmcCounts.get(diagnosis))
            .map(sampleCounts -> Stratum.of(diagnosis, patientCount, sampleCounts)));
  }

  /**
   * Takes the sample-diagnosis stratifier which stratifies samples by pairs of diagnosis and sample
   * type and returns a map from diagnosis to a {@link IntPair pair} of plasma and PBMC counts.
   *
   * @param sampleDiagnosisStratifier the stratifier to process
   * @return a map from diagnosis to a pair of plasma and PBMC counts
   */
  static Map<String, IntPair> diagnosisToPlasmaPbmcCounts(
      Stratifier sampleDiagnosisStratifier) {
    return sampleDiagnosisStratifier.stratum().stream()
        .flatMap(stratum -> sampleStratum(stratum).stream())
        .collect(Collectors.toMap(SampleStratum::diagnosis,
            s -> switch (s.type) {
              case BLOOD_PLASMA -> IntPair.of(s.count, 0);
              case PERIPHERAL_BLOOD_CELLS_VITAL -> IntPair.of(0, s.count);
              default -> IntPair.ZERO;
            },
            IntPair::plus));
  }

  private static Optional<SampleStratum> sampleStratum(Stratifier.Stratum stratum) {
    return Optionals.flatMap(stratum.findComponent(SAMPLE_DIAGNOSIS_STRATIFIER_PREDICATE)
            .map(Component::value)
            .flatMap(CodeableConcept::text),
        stratum.findComponent(SAMPLE_TYPE_STRATIFIER_PREDICATE)
            .map(Component::value)
            .flatMap(CodeableConcept::text),
        (diagnosis, type) -> stratum.findPopulation(INITIAL_POPULATION_PREDICATE)
            .flatMap(Stratifier.Stratum.Population::count)
            .map(count -> new SampleStratum(diagnosis, type, count)));
  }

  private record SampleStratum(String diagnosis, String type, int count) {

    private SampleStratum {
      Objects.requireNonNull(diagnosis);
      Objects.requireNonNull(type);
    }
  }
}
