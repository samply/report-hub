package de.samply.reporthub.service;

import static de.samply.reporthub.model.fhir.Assertions.assertThat;
import static de.samply.reporthub.model.fhir.TaskStatus.READY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.samply.reporthub.dktk.model.fhir.TaskCode;
import de.samply.reporthub.dktk.model.fhir.TaskInput;
import de.samply.reporthub.dktk.model.fhir.TaskOutput;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.MeasureReportStatus;
import de.samply.reporthub.model.fhir.MeasureReportType;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.StringElement;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.model.fhir.TaskStatus;
import de.samply.reporthub.service.fhir.store.DataStore;
import de.samply.reporthub.service.fhir.store.TaskStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EvaluateMeasureServiceTest {

  private static final String TASK_ID = "task-id-153311";
  private static final Task READY_TASK = Task.ready()
      .withId(TASK_ID)
      .withCode(CodeableConcept.coding(TaskCode.EVALUATE_MEASURE.coding()))
      .build();
  private static final String ERROR_MSG = "error-msg-161027";
  private static final String MEASURE_URL = "measure-url-162504";
  private static final String MEASURE_REPORT_ID = "measure-report-id-165832";
  private static final MeasureReport MEASURE_REPORT = MeasureReport.builder(
          MeasureReportStatus.COMPLETE.code(),
          MeasureReportType.SUMMARY.code(),
          Canonical.valueOf(MEASURE_URL))
      .build();

  @Mock
  private TaskStore taskStore;

  @Mock
  private DataStore dataStore;

  private EvaluateMeasureService service;

  @BeforeEach
  void setUp() {
    var clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    service = new EvaluateMeasureService(taskStore, dataStore, clock);
  }

  @Test
  void pipeline_errorOnListReadyTasks() {
    when(taskStore.listTasks(TaskCode.EVALUATE_MEASURE, Instant.EPOCH, READY)).thenReturn(
        Flux.error(
            new Exception(ERROR_MSG)));

    var pipeline = service.pipeline();

    StepVerifier.create(pipeline).expectErrorMessage(ERROR_MSG).verify();
  }

  @Test
  void pipeline_errorOnTaskUpdate() {
    when(taskStore.listTasks(TaskCode.EVALUATE_MEASURE, Instant.EPOCH, READY)).thenReturn(
        Flux.just(READY_TASK));
    when(taskStore.updateTask(any())).thenReturn(Mono.error(new Exception(ERROR_MSG)));

    var pipeline = service.pipeline();

    StepVerifier.create(pipeline).expectErrorMessage(ERROR_MSG).verify();
  }

  @Test
  void pipeline_failedTaskOnMissingMeasureUrl() {
    when(taskStore.listTasks(TaskCode.EVALUATE_MEASURE, Instant.EPOCH, READY)).thenReturn(
        Flux.just(READY_TASK));
    when(taskStore.updateTask(any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

    var pipeline = service.pipeline();

    StepVerifier.create(pipeline)
        .expectNextMatches(task -> TaskStatus.FAILED.test(task.status()))
        .thenCancel()
        .verify();
  }

  /**
   * Tests that errors occurring during task update are propagated.
   */
  @Test
  void processTask_withTaskUpdateError() {
    when(taskStore.updateTask(any())).thenReturn(Mono.error(new Exception(ERROR_MSG)));

    var result = service.processTask(READY_TASK);

    StepVerifier.create(result).expectErrorMessage(ERROR_MSG).verify();
  }

  /**
   * Tests that a failed task is returned if the measure URL is missing.
   */
  @Test
  void processTask_missingMeasureUrl() {
    when(taskStore.updateTask(any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

    var task = service.processTask(READY_TASK).block();

    assertThat(task)
        .hasStatus(TaskStatus.FAILED)
        .containsOutput(CodeableConcept.containsCoding(TaskOutput.ERROR),
            StringElement.valueOf("Missing Measure URL in Task input."));
  }

  /**
   * Tests that a failed task is returned if the measure evaluation fails.
   */
  @Test
  void processTask_failingEvaluateMeasure() {
    var readyTask = READY_TASK.addInput(Input.of(TaskInput.MEASURE.coding(),
        Canonical.valueOf(MEASURE_URL)));
    when(taskStore.updateTask(any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));
    when(dataStore.evaluateMeasure(MEASURE_URL)).thenReturn(Mono.error(new Exception(ERROR_MSG)));

    var task = service.processTask(readyTask).block();

    assertThat(task)
        .hasStatus(TaskStatus.FAILED)
        .containsOutput(CodeableConcept.containsCoding(TaskOutput.ERROR),
            StringElement.valueOf(ERROR_MSG));
  }

  /**
   * Tests that a failed task is returned if the measure report storage fails.
   */
  @Test
  void processTask_failingMeasureReportStorage() {
    var readyTask = Task.ready()
        .withId(TASK_ID)
        .withCode(CodeableConcept.coding(TaskCode.EVALUATE_MEASURE.coding()))
        .withInput(List.of(
            Input.of(TaskInput.MEASURE.coding(), Canonical.valueOf(MEASURE_URL))
        ))
        .build();
    when(taskStore.updateTask(any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));
    when(dataStore.evaluateMeasure(MEASURE_URL)).thenReturn(Mono.just(MEASURE_REPORT));
    when(taskStore.createMeasureReport(MEASURE_REPORT)).thenReturn(
        Mono.error(new Exception(ERROR_MSG)));

    var task = service.processTask(readyTask).block();

    assertThat(task)
        .hasStatus(TaskStatus.FAILED)
        .containsOutput(CodeableConcept.containsCoding(TaskOutput.ERROR),
            StringElement.valueOf(ERROR_MSG));
  }

  /**
   * Tests that a completed task is returned if the measure evaluation succeeds.
   */
  @Test
  void processTask_successfulEvaluateMeasure() {
    var readyTask = Task.ready()
        .withId(TASK_ID)
        .withCode(CodeableConcept.coding(TaskCode.EVALUATE_MEASURE.coding()))
        .withInput(List.of(
            Input.of(TaskInput.MEASURE.coding(), Canonical.valueOf(MEASURE_URL))
        ))
        .build();
    when(taskStore.updateTask(any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));
    when(dataStore.evaluateMeasure(MEASURE_URL)).thenReturn(Mono.just(MEASURE_REPORT));
    when(taskStore.createMeasureReport(MEASURE_REPORT)).thenReturn(Mono.just(
        MEASURE_REPORT.withId(MEASURE_REPORT_ID)));

    var task = service.processTask(readyTask).block();

    assertThat(task)
        .hasStatus(TaskStatus.COMPLETED)
        .containsOutput(CodeableConcept.containsCoding(TaskOutput.MEASURE_REPORT),
            Reference.ofReference("MeasureReport", MEASURE_REPORT_ID));
  }
}
