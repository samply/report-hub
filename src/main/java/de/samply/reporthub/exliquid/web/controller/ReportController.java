package de.samply.reporthub.exliquid.web.controller;

import de.samply.reporthub.exliquid.web.model.Report;
import de.samply.reporthub.exliquid.web.model.Report.Stratum;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.MeasureReport.Group;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Population;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Stratifier;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Stratifier.Stratum.Component;
import de.samply.reporthub.service.TaskStore;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller("exliquidReportController")
public class ReportController {

  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

  public static final Predicate<CodeableConcept> PATIENT_GROUP_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-measure-group", "patient");
  public static final Predicate<CodeableConcept> SPECIMEN_GROUP_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-measure-group", "specimen");
  public static final Predicate<CodeableConcept> INITIAL_POPULATION_PREDICATE = CodeableConcept.containsCoding(
      "http://terminology.hl7.org/CodeSystem/measure-population", "initial-population");
  public static final Predicate<CodeableConcept> DIAGNOSIS_STRATIFIER_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-stratifier", "diagnosis");
  public static final Predicate<CodeableConcept> SAMPLE_DIAGNOSIS_STRATIFIER_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-stratifier", "sample-diagnosis");
  public static final Predicate<CodeableConcept> SAMPLE_TYPE_STRATIFIER_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-stratifier", "sample-type");

  private final TaskStore taskStore;

  public ReportController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  @GetMapping("exliquid-report/{id}")
  public String index(@PathVariable("id") String id, Model model) {
    model.addAttribute("report",
        taskStore.fetchMeasureReport(id).map(this::convert).flatMap(Mono::justOrEmpty));
    return "exliquid/report";
  }

  /**
   * Converts a FHIR {@link MeasureReport} {@code report} into {@link Report} used for EXLIQUID.
   *
   * @param report the FHIR {@code MeasureReport} to convert
   * @return an {@code Optional} of the converted {@link Report} or an empty {@code Optional} if the
   * conversion was not possible
   */
  private Optional<Report> convert(MeasureReport report) {
    return report.date()
        .flatMap(date -> report.findGroup(PATIENT_GROUP_PREDICATE)
            .flatMap(patientGroup -> report.findGroup(SPECIMEN_GROUP_PREDICATE)
                .flatMap(specimenGroup -> convert(date, patientGroup, specimenGroup))));
  }

  private Optional<Report> convert(OffsetDateTime date, Group patientGroup, Group specimenGroup) {
    return patientGroup.findPopulation(INITIAL_POPULATION_PREDICATE).flatMap(Population::count)
        .flatMap(totalNumberOfPatients -> patientGroup
            .findStratifier(DIAGNOSIS_STRATIFIER_PREDICATE)
            .flatMap(diagnosisStratifier -> specimenGroup
                .findPopulation(INITIAL_POPULATION_PREDICATE).flatMap(Population::count)
                .flatMap(totalNumberOfSpecimen -> specimenGroup
                    .findStratifier(SAMPLE_DIAGNOSIS_STRATIFIER_PREDICATE)
                    .map(sampleDiagnosisStratifier -> new Report(date,
                        totalNumberOfPatients, totalNumberOfSpecimen,
                        strata(diagnosisStratifier, sampleDiagnosisStratifier))))));
  }

  private List<Stratum> strata(Stratifier patientDiagnosisStratifier,
      Stratifier sampleDiagnosisStratifier) {
    var map = sampleDiagnosisStratifier.stratum().stream()
        .flatMap(stratum -> stratum.findComponent(SAMPLE_DIAGNOSIS_STRATIFIER_PREDICATE)
            .map(Component::value)
            .flatMap(CodeableConcept::text)
            .flatMap(diagnosis -> stratum.findComponent(SAMPLE_TYPE_STRATIFIER_PREDICATE)
                .map(Component::value)
                .flatMap(CodeableConcept::text)
                .flatMap(type -> stratum.findPopulation(INITIAL_POPULATION_PREDICATE)
                    .flatMap(Stratifier.Stratum.Population::count)
                    .map(count -> new SampleStratum(diagnosis, type, count)))).stream())
        .collect(Collectors.toMap(SampleStratum::diagnosis, s -> switch (s.type) {
          case "blood-plasma" -> new SampleStratum2(s.diagnosis, s.count, 0);
          case "peripheral-blood-cells-vital" -> new SampleStratum2(s.diagnosis, 0, s.count);
          default -> new SampleStratum2(s.diagnosis, 0, 0);
        }, (s1, s2) -> new SampleStratum2(s1.diagnosis, s1.plasmaCount + s2.plasmaCount,
            s1.pbmcCount + s2.pbmcCount)));

    return patientDiagnosisStratifier.stratum().stream()
        .flatMap(stratum -> stratum.value().flatMap(CodeableConcept::text)
            .flatMap(diagnosis -> stratum.findPopulation(INITIAL_POPULATION_PREDICATE)
                .flatMap(Stratifier.Stratum.Population::count)
                .flatMap(patientCount -> Optional.ofNullable(map.get(diagnosis))
                    .map(s -> new Stratum(diagnosis, patientCount, s.plasmaCount, s.pbmcCount))))
            .stream())
        .toList();
  }

  private record SampleStratum(String diagnosis, String type, int count) {

    private SampleStratum {
      Objects.requireNonNull(diagnosis);
      Objects.requireNonNull(type);
    }
  }

  private record SampleStratum2(String diagnosis, int plasmaCount, int pbmcCount) {

    private SampleStratum2 {
      Objects.requireNonNull(diagnosis);
    }
  }
}
