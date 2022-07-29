package de.samply.reporthub.web.controller;

import static de.samply.reporthub.web.controller.ServerResponseAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.PublicationStatus;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.TaskStatus;
import de.samply.reporthub.service.ResourceNotFoundException;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.web.model.Link;
import de.samply.reporthub.web.model.WebTask;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

  private static final String TASK_ID = "task-id-174748";
  private static final String ACTIVITY_DEFINITION_ID = "activity-definition-id-181703";
  private static final String ACTIVITY_DEFINITION_URL = "activity-definition-url-180458";
  private static final String ACTIVITY_DEFINITION_TITLE = "activity-definition-title-180728";
  private static final ActivityDefinition ACTIVITY_DEFINITION = ActivityDefinition.builder(
          PublicationStatus.DRAFT.code())
      .withId(ACTIVITY_DEFINITION_ID)
      .withTitle(ACTIVITY_DEFINITION_TITLE)
      .build();
  private static final Link ACTIVITY_DEFINITION_LINK =
      new Link(URI.create("activity-definition/" + ACTIVITY_DEFINITION_ID),
          ACTIVITY_DEFINITION_TITLE);
  private static final OffsetDateTime TASK_LAST_MODIFIED = OffsetDateTime.ofInstant(Instant.EPOCH,
      ZoneOffset.UTC);
  private static final Task TASK = Task.builder(TaskStatus.DRAFT.code())
      .withId(TASK_ID)
      .withInstantiatesCanonical(ACTIVITY_DEFINITION_URL)
      .withLastModified(TASK_LAST_MODIFIED)
      .build();

  @Mock
  private TaskStore taskStore;

  @InjectMocks
  private TaskController controller;

  @Test
  void handle_minimal() {
    var request = mock(ServerRequest.class);
    when(request.pathVariable("id")).thenReturn(TASK_ID);
    when(request.uriBuilder()).thenAnswer(invocation -> UriComponentsBuilder.newInstance());
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(TASK));
    when(taskStore.findActivityDefinitionByUrl(ACTIVITY_DEFINITION_URL))
        .thenReturn(Mono.just(ACTIVITY_DEFINITION));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.empty());

    var response = controller.handle(request).block();

    assertThat(response)
        .hasStatusCode(OK)
        .isRendering()
        .hasName("task")
        .hasModelEntrySatisfying("task", WebTask.class,
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
        .containsModelEntry("error", "The task with id `%s` was not found.".formatted(TASK_ID));
  }

  @Test
  void webTask() {
    var request = mock(ServerRequest.class);
    when(request.uriBuilder()).thenAnswer(invocation -> UriComponentsBuilder.newInstance());
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(TASK));
    when(taskStore.findActivityDefinitionByUrl(ACTIVITY_DEFINITION_URL))
        .thenReturn(Mono.just(ACTIVITY_DEFINITION));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.empty());

    var mono = controller.webTask(request, TASK_ID);

    StepVerifier.create(mono).assertNext(webTask -> {
          assertThat(webTask.id()).isEqualTo(TASK_ID);
          assertThat(webTask.activityDefinitionLink()).isEqualTo(ACTIVITY_DEFINITION_LINK);
          assertThat(webTask.status()).isEqualTo("draft");
          assertThat(webTask.lastModified()).isEqualTo(TASK_LAST_MODIFIED);
          assertThat(webTask.reportLink()).isEmpty();
          assertThat(webTask.history()).isEmpty();
        }
    ).verifyComplete();
  }

  /**
   * Returns the WebTask even if fetching the history resulted in an error.
   */
  @Test
  void webTask_historyError() {
    var request = mock(ServerRequest.class);
    when(request.uriBuilder()).thenAnswer(invocation -> UriComponentsBuilder.newInstance());
    when(taskStore.fetchTask(TASK_ID)).thenReturn(Mono.just(TASK));
    when(taskStore.findActivityDefinitionByUrl(ACTIVITY_DEFINITION_URL))
        .thenReturn(Mono.just(ACTIVITY_DEFINITION));
    when(taskStore.fetchTaskHistory(TASK_ID)).thenReturn(Flux.error(new Exception()));

    var mono = controller.webTask(request, TASK_ID);

    StepVerifier.create(mono).assertNext(webTask -> {
          assertThat(webTask.id()).isEqualTo(TASK_ID);
          assertThat(webTask.history()).isEmpty();
        }
    ).verifyComplete();
  }
}
