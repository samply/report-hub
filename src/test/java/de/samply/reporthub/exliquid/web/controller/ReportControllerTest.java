package de.samply.reporthub.exliquid.web.controller;

import static de.samply.reporthub.exliquid.web.controller.ReportController.BLOOD_PLASMA;
import static de.samply.reporthub.exliquid.web.controller.ReportController.EXLIQUID_MEASURE_GROUP;
import static de.samply.reporthub.exliquid.web.controller.ReportController.EXLIQUID_STRATIFIER;
import static de.samply.reporthub.exliquid.web.controller.ReportController.MEASURE_POPULATION;
import static de.samply.reporthub.exliquid.web.controller.ReportController.PERIPHERAL_BLOOD_CELLS_VITAL;
import static de.samply.reporthub.exliquid.web.controller.ReportController.convert;
import static de.samply.reporthub.web.controller.ServerResponseAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import de.samply.reporthub.exliquid.web.model.Report;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Coding;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.MeasureReport.Group;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Population;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Stratifier;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Stratifier.Stratum;
import de.samply.reporthub.model.fhir.MeasureReport.Group.Stratifier.Stratum.Component;
import de.samply.reporthub.model.fhir.MeasureReportStatus;
import de.samply.reporthub.model.fhir.MeasureReportType;
import de.samply.reporthub.service.ResourceNotFoundException;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.util.IntPair;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

  private static final String ID = "id-180016";
  private static final CodeableConcept PATIENT_GROUP_CODE =
      CodeableConcept.of(Coding.of(EXLIQUID_MEASURE_GROUP, "patient"));
  private static final CodeableConcept SPECIMEN_GROUP_CODE =
      CodeableConcept.of(Coding.of(EXLIQUID_MEASURE_GROUP, "specimen"));
  private static final CodeableConcept INITIAL_POPULATION_CODE =
      CodeableConcept.of(Coding.of(MEASURE_POPULATION, "initial-population"));
  private static final CodeableConcept DIAGNOSIS_STRATIFIER_CODE =
      CodeableConcept.of(Coding.of(EXLIQUID_STRATIFIER, "diagnosis"));
  private static final CodeableConcept SAMPLE_DIAGNOSIS_STRATIFIER_CODE =
      CodeableConcept.of(Coding.of(EXLIQUID_STRATIFIER, "sample-diagnosis"));
  private static final CodeableConcept SAMPLE_TYPE_STRATIFIER_CODE =
      CodeableConcept.of(Coding.of(EXLIQUID_STRATIFIER, "sample-type"));
  private static final MeasureReport EMPTY_MEASURE_REPORT = MeasureReport.builder(
          MeasureReportStatus.COMPLETE.code(),
          MeasureReportType.INDIVIDUAL.code(),
          Canonical.valueOf("foo"))
      .build();
  private static final MeasureReport MINIMAL_MEASURE_REPORT = MeasureReport.builder(
          MeasureReportStatus.COMPLETE.code(),
          MeasureReportType.INDIVIDUAL.code(),
          Canonical.valueOf("foo"))
      .withDate(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
      .withGroup(List.of(
          Group.builder()
              .withCode(PATIENT_GROUP_CODE)
              .withPopulation(List.of(Population.of(INITIAL_POPULATION_CODE, 23)))
              .withStratifier(List.of(
                  Stratifier.builder()
                      .withCode(List.of(DIAGNOSIS_STRATIFIER_CODE))
                      .build()
              ))
              .build(),
          Group.builder()
              .withCode(SPECIMEN_GROUP_CODE)
              .withPopulation(List.of(Population.of(INITIAL_POPULATION_CODE, 42)))
              .withStratifier(List.of(
                  Stratifier.builder()
                      .withCode(List.of(
                          SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
                          SAMPLE_TYPE_STRATIFIER_CODE))
                      .build()
              ))
              .build()
      ))
      .build();

  @Mock
  private TaskStore taskStore;

  @InjectMocks
  private ReportController controller;

  @Test
  void handler_minimal() {
    var request = mock(ServerRequest.class);
    when(request.pathVariable("id")).thenReturn(ID);
    when(taskStore.fetchMeasureReport(ID)).thenReturn(Mono.just(MINIMAL_MEASURE_REPORT));

    var response = controller.handler(request).block();

    assertThat(response)
        .hasStatusCode(OK)
        .isRendering()
        .hasName("exliquid/report")
        .containsModelEntry("report", convert(MINIMAL_MEASURE_REPORT).orElseThrow());
  }

  @Test
  void handler_404() {
    var request = mock(ServerRequest.class);
    when(request.pathVariable("id")).thenReturn(ID);
    when(taskStore.fetchMeasureReport(ID))
        .thenReturn(Mono.error(new ResourceNotFoundException("MeasureReport", ID)));

    var response = controller.handler(request).block();

    assertThat(response)
        .hasStatusCode(OK)
        .isRendering()
        .hasName("404")
        .containsModelEntry("error",
            "The EXLIQUID report with id `%s` was not found.".formatted(ID));
  }

  @Test
  void convert_empty() {
    var report = convert(EMPTY_MEASURE_REPORT);

    assertThat(report).isEmpty();
  }

  @Test
  void convert_minimal() {
    var report = convert(MINIMAL_MEASURE_REPORT);

    assertThat(report).isPresent();
    assertThat(report.map(Report::totalNumberOfPatients)).contains(23);
    assertThat(report.map(Report::totalNumberOfSpecimen)).contains(42);
    assertThat(report.map(Report::strata)).contains(List.of());
  }

  @Test
  void convert_full() {
    var measureReport = MeasureReport.builder(MeasureReportStatus.COMPLETE.code(),
            MeasureReportType.SUMMARY.code(),
            Canonical.valueOf("foo"))
        .withDate(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
        .withGroup(List.of(
            Group.builder()
                .withCode(PATIENT_GROUP_CODE)
                .withPopulation(List.of(Population.of(INITIAL_POPULATION_CODE, 23)))
                .withStratifier(List.of(
                    Stratifier.builder()
                        .withCode(List.of(DIAGNOSIS_STRATIFIER_CODE))
                        .withStratum(List.of(
                            Stratum.builder()
                                .withValue(CodeableConcept.of("C25"))
                                .withPopulation(List.of(
                                    Stratum.Population.of(INITIAL_POPULATION_CODE, 23)))
                                .build()
                        ))
                        .build()
                ))
                .build(),
            Group.builder()
                .withCode(SPECIMEN_GROUP_CODE)
                .withPopulation(List.of(Population.of(INITIAL_POPULATION_CODE, 42)))
                .withStratifier(List.of(
                    Stratifier.builder()
                        .withCode(List.of(
                            SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
                            SAMPLE_TYPE_STRATIFIER_CODE))
                        .withStratum(List.of(
                            Stratum.builder()
                                .withComponent(List.of(
                                    Component.of(
                                        SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
                                        CodeableConcept.of("C25")),
                                    Component.of(
                                        SAMPLE_TYPE_STRATIFIER_CODE,
                                        CodeableConcept.of(BLOOD_PLASMA))))
                                .withPopulation(List.of(
                                    Stratum.Population.of(INITIAL_POPULATION_CODE, 20)))
                                .build(),
                            Stratum.builder()
                                .withComponent(List.of(
                                    Component.of(
                                        SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
                                        CodeableConcept.of("C25")),
                                    Component.of(
                                        SAMPLE_TYPE_STRATIFIER_CODE,
                                        CodeableConcept.of(PERIPHERAL_BLOOD_CELLS_VITAL))))
                                .withPopulation(List.of(
                                    Stratum.Population.of(INITIAL_POPULATION_CODE, 22)))
                                .build()
                        ))
                        .build()
                ))
                .build()
        ))
        .build();

    var report = convert(measureReport);

    assertThat(report).isPresent();
    assertThat(report.map(Report::totalNumberOfPatients)).contains(23);
    assertThat(report.map(Report::totalNumberOfSpecimen)).contains(42);
    assertThat(report.map(Report::strata)).hasValueSatisfying(strata ->
        assertThat(strata).allSatisfy(stratum ->
            assertThat(stratum).isEqualTo(new Report.Stratum("C25", 23, 20, 22))));
  }

  @Test
  void diagnosisToPlasmaPbmcCounts_empty() {
    var stratifier = Stratifier.builder().build();

    var map = ReportController.diagnosisToPlasmaPbmcCounts(stratifier);

    assertThat(map).isEmpty();
  }

  @Test
  void diagnosisToPlasmaPbmcCounts_withBloodPlasma() {
    var stratifier = Stratifier.builder()
        .withCode(List.of(
            SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
            SAMPLE_TYPE_STRATIFIER_CODE))
        .withStratum(List.of(
            Stratum.builder()
                .withComponent(List.of(
                    Component.of(SAMPLE_DIAGNOSIS_STRATIFIER_CODE, CodeableConcept.of("C25")),
                    Component.of(SAMPLE_TYPE_STRATIFIER_CODE, CodeableConcept.of(BLOOD_PLASMA))))
                .withPopulation(List.of(Stratum.Population.of(INITIAL_POPULATION_CODE, 23)))
                .build()
        ))
        .build();

    var map = ReportController.diagnosisToPlasmaPbmcCounts(stratifier);

    assertThat(map).allSatisfy((diagnosis, counts) -> {
      assertThat(diagnosis).isEqualTo("C25");
      assertThat(counts).isEqualTo(IntPair.of(23, 0));
    });
  }

  @Test
  void diagnosisToPlasmaPbmcCounts_withEmptyBloodPlasma() {
    var stratifier = Stratifier.builder()
        .withCode(List.of(
            SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
            SAMPLE_TYPE_STRATIFIER_CODE))
        .withStratum(List.of(
            Stratum.builder()
                .withComponent(List.of(
                    Component.of(SAMPLE_DIAGNOSIS_STRATIFIER_CODE, CodeableConcept.of("C25")),
                    Component.of(SAMPLE_TYPE_STRATIFIER_CODE, CodeableConcept.of(BLOOD_PLASMA))))
                .build()
        ))
        .build();

    var map = ReportController.diagnosisToPlasmaPbmcCounts(stratifier);

    assertThat(map).isEmpty();
  }

  @Test
  void diagnosisToPlasmaPbmcCounts_withPbmc() {
    var stratifier = Stratifier.builder()
        .withCode(List.of(
            SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
            SAMPLE_TYPE_STRATIFIER_CODE))
        .withStratum(List.of(
            Stratum.builder()
                .withComponent(List.of(
                    Component.of(SAMPLE_DIAGNOSIS_STRATIFIER_CODE, CodeableConcept.of("C25")),
                    Component.of(SAMPLE_TYPE_STRATIFIER_CODE,
                        CodeableConcept.of(PERIPHERAL_BLOOD_CELLS_VITAL))))
                .withPopulation(List.of(Stratum.Population.of(INITIAL_POPULATION_CODE, 23)))
                .build()
        ))
        .build();

    var map = ReportController.diagnosisToPlasmaPbmcCounts(stratifier);

    assertThat(map).allSatisfy((diagnosis, counts) -> {
      assertThat(diagnosis).isEqualTo("C25");
      assertThat(counts).isEqualTo(IntPair.of(0, 23));
    });
  }

  @Test
  void diagnosisToPlasmaPbmcCounts_withOtherSampleType() {
    var stratifier = Stratifier.builder()
        .withCode(List.of(
            SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
            SAMPLE_TYPE_STRATIFIER_CODE))
        .withStratum(List.of(
            Stratum.builder()
                .withComponent(List.of(
                    Component.of(SAMPLE_DIAGNOSIS_STRATIFIER_CODE, CodeableConcept.of("C25")),
                    Component.of(SAMPLE_TYPE_STRATIFIER_CODE, CodeableConcept.of("foo"))))
                .withPopulation(List.of(Stratum.Population.of(INITIAL_POPULATION_CODE, 23)))
                .build()
        ))
        .build();

    var map = ReportController.diagnosisToPlasmaPbmcCounts(stratifier);

    assertThat(map).allSatisfy((diagnosis, counts) -> {
      assertThat(diagnosis).isEqualTo("C25");
      assertThat(counts).isEqualTo(IntPair.of(0, 0));
    });
  }

  @Test
  void diagnosisToPlasmaPbmcCounts_withBothSampleTypes() {
    var stratifier = Stratifier.builder()
        .withCode(List.of(
            SAMPLE_DIAGNOSIS_STRATIFIER_CODE,
            SAMPLE_TYPE_STRATIFIER_CODE))
        .withStratum(List.of(
            Stratum.builder()
                .withComponent(List.of(
                    Component.of(SAMPLE_DIAGNOSIS_STRATIFIER_CODE, CodeableConcept.of("C25")),
                    Component.of(SAMPLE_TYPE_STRATIFIER_CODE,
                        CodeableConcept.of(BLOOD_PLASMA))))
                .withPopulation(List.of(Stratum.Population.of(INITIAL_POPULATION_CODE, 23)))
                .build(),
            Stratum.builder()
                .withComponent(List.of(
                    Component.of(SAMPLE_DIAGNOSIS_STRATIFIER_CODE, CodeableConcept.of("C25")),
                    Component.of(SAMPLE_TYPE_STRATIFIER_CODE,
                        CodeableConcept.of(PERIPHERAL_BLOOD_CELLS_VITAL))))
                .withPopulation(List.of(Stratum.Population.of(INITIAL_POPULATION_CODE, 42)))
                .build()
        ))
        .build();

    var map = ReportController.diagnosisToPlasmaPbmcCounts(stratifier);

    assertThat(map).allSatisfy((diagnosis, counts) -> {
      assertThat(diagnosis).isEqualTo("C25");
      assertThat(counts).isEqualTo(IntPair.of(23, 42));
    });
  }
}
