package de.samply.reporthub.exliquid.component;

import de.samply.reporthub.dktk.model.fhir.TaskCode;
import de.samply.reporthub.dktk.model.fhir.TaskInput;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.service.TaskCreator;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("https://dktk.dkfz.de/fhir/ActivityDefinition/generate-exliquid-dashboard-report")
public class GenerateDashboardReportTaskCreator implements TaskCreator {

  private static final String GENERATE_DASHBOARD_REPORT_URL =
      "https://dktk.dkfz.de/fhir/ActivityDefinition/generate-exliquid-dashboard-report";
  private static final Canonical MEASURE_URL = Canonical.valueOf(
      "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard");

  private final Clock clock;

  public GenerateDashboardReportTaskCreator(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Mono<Task> create(ActivityDefinition activityDefinition) {
    return Mono.just(Task.ready()
        .withInstantiatesCanonical(GENERATE_DASHBOARD_REPORT_URL)
        .withCode(CodeableConcept.coding(TaskCode.EVALUATE_MEASURE.coding()))
        .withLastModified(OffsetDateTime.now(clock))
        .withInput(List.of(Input.of(TaskInput.MEASURE.coding(), MEASURE_URL)))
        .build());
  }
}
