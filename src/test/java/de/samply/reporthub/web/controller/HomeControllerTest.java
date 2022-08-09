package de.samply.reporthub.web.controller;

import static de.samply.reporthub.model.fhir.PublicationStatus.DRAFT;
import static de.samply.reporthub.model.fhir.TaskStatus.REQUESTED;
import static de.samply.reporthub.web.controller.ServerResponseAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.SEE_OTHER;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.web.model.CreateTaskFormActivityDefinition;
import de.samply.reporthub.web.model.Link;
import de.samply.reporthub.web.model.WebTask;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

  private static final String ACTIVITY_DEFINITION_ID = "id-155958";
  private static final String ACTIVITY_DEFINITION_URL = "url-155958";
  private static final String ACTIVITY_DEFINITION_TITLE = "title-175807";
  private static final String TASK_ID = "id-160606";
  public static final OffsetDateTime LAST_MODIFIED = OffsetDateTime.ofInstant(Instant.EPOCH,
      ZoneOffset.UTC);

  @Mock
  private TaskStore taskStore;

  private HomeController controller;

  @BeforeEach
  void setUp() {
    controller = new HomeController(taskStore, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
  }

  @Test
  void homeModel_empty() {
    var request = mock(ServerRequest.class);
    when(taskStore.listAllActivityDefinitions()).thenReturn(Flux.empty());
    when(taskStore.listNewestTasks()).thenReturn(Flux.empty());

    var model = controller.homeModel(request).block();

    assertThat(model)
        .containsEntry("createTaskFormActivityDefinitions", List.of())
        .containsEntry("tasks", List.of());
  }

  @Test
  void homeModel_oneActivityDefinition() {
    var request = mock(ServerRequest.class);
    var activityDefinition = ActivityDefinition.builder(DRAFT.code())
        .withId(ACTIVITY_DEFINITION_ID)
        .withUrl(ACTIVITY_DEFINITION_URL)
        .withTitle(ACTIVITY_DEFINITION_TITLE)
        .build();
    when(taskStore.listAllActivityDefinitions()).thenReturn(Flux.just(activityDefinition));
    when(taskStore.listNewestTasks()).thenReturn(Flux.empty());

    var model = controller.homeModel(request).block();

    assertThat(model)
        .containsEntry("createTaskFormActivityDefinitions", List.of(
            new CreateTaskFormActivityDefinition(ACTIVITY_DEFINITION_URL,
                ACTIVITY_DEFINITION_TITLE)))
        .containsEntry("tasks", List.of());
  }

  @Test
  void homeModel_oneActivityDefinitionAndOneTask() {
    var request = mock(ServerRequest.class);
    var activityDefinition = ActivityDefinition.builder(DRAFT.code())
        .withId(ACTIVITY_DEFINITION_ID)
        .withUrl(ACTIVITY_DEFINITION_URL)
        .withTitle(ACTIVITY_DEFINITION_TITLE)
        .build();
    var task = Task.builder(REQUESTED.code())
        .withId(TASK_ID)
        .withInstantiatesCanonical(ACTIVITY_DEFINITION_URL)
        .withLastModified(LAST_MODIFIED)
        .build();
    when(request.uriBuilder()).thenAnswer(invocation -> UriComponentsBuilder.newInstance());
    when(taskStore.listAllActivityDefinitions()).thenReturn(Flux.just(activityDefinition));
    when(taskStore.listNewestTasks()).thenReturn(Flux.just(task));

    var model = controller.homeModel(request).block();

    assertThat(model)
        .containsEntry("createTaskFormActivityDefinitions", List.of(
            new CreateTaskFormActivityDefinition(ACTIVITY_DEFINITION_URL,
                ACTIVITY_DEFINITION_TITLE)))
        .containsEntry("tasks", List.of(new WebTask(TASK_ID, new Link(URI.create(
            "activity-definition/" + ACTIVITY_DEFINITION_ID), ACTIVITY_DEFINITION_TITLE),
            "requested", LAST_MODIFIED, Optional.empty(), List.of())));
  }

  @Test
  void createTask() {
    var request = mock(ServerRequest.class);
    var formData = new LinkedMultiValueMap<>(Map.of("instantiates",
        List.of(ACTIVITY_DEFINITION_URL)));
    when(request.formData()).thenReturn(Mono.just(formData));
    when(request.uriBuilder()).thenAnswer(
        invocation -> UriComponentsBuilder.fromUriString("/create-task"));
    var taskToCreate = controller.requestedTask(formData);
    when(taskStore.createTask(taskToCreate)).thenReturn(Mono.just(taskToCreate.withId(TASK_ID)));

    var response = controller.createTask(request).block();

    assertThat(response)
        .hasStatusCode(SEE_OTHER)
        .hasLocation(URI.create("/"));
  }
}
