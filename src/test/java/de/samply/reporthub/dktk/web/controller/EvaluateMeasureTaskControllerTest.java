package de.samply.reporthub.dktk.web.controller;

import static de.samply.reporthub.web.controller.ServerResponseAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import de.samply.reporthub.dktk.model.fhir.TaskInput;
import de.samply.reporthub.dktk.model.fhir.TaskOutput;
import de.samply.reporthub.dktk.web.model.EvaluateMeasureTask;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.Measure;
import de.samply.reporthub.model.fhir.StringElement;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.model.fhir.Task.Output;
import de.samply.reporthub.service.fhir.store.DataStore;
import de.samply.reporthub.service.fhir.store.ResourceNotFoundException;
import de.samply.reporthub.service.fhir.store.TaskStore;
import java.net.URI;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EvaluateMeasureTaskControllerTest {

  private static final String TASK_ID = "task-id-174748";
  private static final String MEASURE_ID = "measure-id-181703";
  private static final String MEASURE_URL = "measure-url-180458";
  private static final String MEASURE_TITLE = "measure-title-180728";
  private static final Measure MEASURE = Measure.draft()
      .withId(MEASURE_ID)
      .withTitle(MEASURE_TITLE)
      .build();
  private static final OffsetDateTime TASK_LAST_MODIFIED = OffsetDateTime.ofInstant(Instant.EPOCH,
      ZoneOffset.UTC);
  private static final Task TASK = Task.draft()
      .withId(TASK_ID)
      .withLastModified(TASK_LAST_MODIFIED)
      .withInput(List.of(Input.of(TaskInput.MEASURE.coding(), Canonical.valueOf(MEASURE_URL))))
      .build();
  public static final String ERROR_MSG = "error-msg-180414";

  @Mock
  private TaskStore taskStore;

  @Mock
  private DataStore dataStore;

  @InjectMocks
  private EvaluateMeasureTaskController controller;

  @Test
  void handle_minimal() {
    var request = mock(ServerRequest.class);
    when(request.pathVariable("id")).thenReturn(TASK_ID);
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(TASK));
    when(dataStore.findByUrl(Measure.class, MEASURE_URL)).thenReturn(Mono.just(MEASURE));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.empty());

    var response = controller.handle(request).block();

    assertThat(response)
        .hasStatusCode(OK)
        .isRendering()
        .hasName("dktk/evaluate-measure-task")
        .hasModelEntrySatisfying("task", EvaluateMeasureTask.class,
            task -> assertThat(task.id()).isEqualTo(TASK_ID));
  }

  @Test
  void handle_taskNotFound() {
    var request = mock(ServerRequest.class);
    when(request.pathVariable("id")).thenReturn(TASK_ID);
    when(taskStore.fetchTask(TASK_ID))
        .thenReturn(Mono.error(new ResourceNotFoundException("Task", TASK_ID)));

    var response = controller.handle(request).block();

    assertThat(response)
        .hasStatusCode(OK)
        .isRendering()
        .hasName("404")
        .containsModelEntry("error", "The Task with id `%s` was not found.".formatted(TASK_ID));
  }

  @Test
  void task_missingMeasureUrl() {
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(Task.draft().build()));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.empty());

    var result = controller.task(TASK_ID);

    StepVerifier.create(result)
        .assertNext(task -> assertThat(task.measureLink()).isEmpty())
        .verifyComplete();
  }

  /**
   * Returns the EvaluateMeasureTask even if fetching the Measure resulted in an error.
   */
  @Test
  void task_measureFetchError() {
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(Task.draft()
        .withInput(List.of(Input.of(TaskInput.MEASURE.coding(), Canonical.valueOf(MEASURE_URL))))
        .build()));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.empty());
    when(dataStore.findByUrl(Measure.class, MEASURE_URL)).thenReturn(Mono.error(new Exception()));

    var result = controller.task(TASK_ID);

    StepVerifier.create(result)
        .assertNext(task -> assertThat(task.measureLink()).hasValueSatisfying(
            link -> {
              assertThat(link.href()).isEqualTo(URI.create(MEASURE_URL));
              assertThat(link.label()).isEqualTo(MEASURE_URL);
            })
        ).verifyComplete();
  }

  @Test
  void task() {
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(TASK));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.empty());
    when(dataStore.findByUrl(Measure.class, MEASURE_URL)).thenReturn(Mono.just(MEASURE));

    var result = controller.task(TASK_ID);

    StepVerifier.create(result).assertNext(task -> {
          assertThat(task.id()).isEqualTo(TASK_ID);
          assertThat(task.status()).isEqualTo("draft");
          assertThat(task.measureLink()).hasValueSatisfying(
              link -> {
                assertThat(link.href()).isEqualTo(URI.create("/dktk/measure/" + MEASURE_ID));
                assertThat(link.label()).isEqualTo(MEASURE_TITLE);
              });
          assertThat(task.reportLink()).isEmpty();
          assertThat(task.history()).isEmpty();
        }
    ).verifyComplete();
  }

  /**
   * Returns the EvaluateMeasureTask even if fetching the history resulted in an error.
   */
  @Test
  void task_historyFetchError() {
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(TASK));
    when(dataStore.findByUrl(Measure.class, MEASURE_URL)).thenReturn(Mono.just(MEASURE));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.error(new Exception()));

    var result = controller.task(TASK_ID);

    StepVerifier.create(result).assertNext(webTask -> {
          assertThat(webTask.id()).isEqualTo(TASK_ID);
          assertThat(webTask.history()).isEmpty();
        }
    ).verifyComplete();
  }

  @Test
  void task_withError() {
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(Task.failed()
        .withOutput(List.of(Output.of(TaskOutput.ERROR.coding(),
            StringElement.valueOf(ERROR_MSG))))
        .build()));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.empty());

    var result = controller.task(TASK_ID);

    StepVerifier.create(result)
        .assertNext(task -> assertThat(task.error()).contains(ERROR_MSG))
        .verifyComplete();
  }
}
