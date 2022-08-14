package de.samply.reporthub.web.controller;

import static de.samply.reporthub.web.controller.ServerResponseAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import de.samply.reporthub.dktk.model.fhir.TaskCode;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.service.TaskCreator;
import de.samply.reporthub.service.fhir.store.TaskStore;
import de.samply.reporthub.web.model.Link;
import de.samply.reporthub.web.model.TaskLineItem;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

  private static final String ACTIVITY_DEFINITION_URL = "url-155958";
  private static final String TASK_ID = "id-160606";
  public static final OffsetDateTime LAST_MODIFIED = OffsetDateTime.ofInstant(Instant.EPOCH,
      ZoneOffset.UTC);
  public static final Canonical MEASURE_URL = Canonical.valueOf(
      "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard");
  public static final ActivityDefinition ACTIVITY_DEFINITION = ActivityDefinition.active()
      .withUrl(ACTIVITY_DEFINITION_URL)
      .build();
  public static final Task TASK = Task.draft().build();

  @Mock
  private TaskStore taskStore;

  @Mock
  private GenericApplicationContext applicationContext;

  @InjectMocks
  private HomeController controller;

  @Test
  void handle_empty() {
    var request = mock(ServerRequest.class);
    when(taskStore.listAllActivityDefinitions()).thenReturn(Flux.empty());
    when(taskStore.listNewestTasks()).thenReturn(Flux.empty());

    var response = controller.handle(request).block();

    assertThat(response)
        .hasStatusCode(OK)
        .isRendering()
        .hasName("home")
        .containsModelEntry("taskLineItems", List.of());
  }

  @Test
  void handle_oneOneTask() {
    var request = mock(ServerRequest.class);
    var task = Task.ready()
        .withId(TASK_ID)
        .withCode(CodeableConcept.coding(TaskCode.EVALUATE_MEASURE.coding()))
        .withLastModified(LAST_MODIFIED)
        .build();
    when(request.uriBuilder()).thenAnswer(invocation -> UriComponentsBuilder.newInstance());
    when(taskStore.listAllActivityDefinitions()).thenReturn(Flux.empty());
    when(taskStore.listNewestTasks()).thenReturn(Flux.just(task));

    var response = controller.handle(request).block();

    assertThat(response)
        .hasStatusCode(OK)
        .isRendering()
        .hasName("home")
        .containsModelEntry("taskLineItems", List.of(new TaskLineItem(LAST_MODIFIED,
            new Link(URI.create("/task/evaluate-measure/" + TASK_ID), TASK_ID), "evaluate-measure",
            "ready")));
  }

  /*@Test
  void createTask() {
    var request = mock(ServerRequest.class);
    var formData = new LinkedMultiValueMap<>(Map.of("instantiates",
        List.of(ACTIVITY_DEFINITION_URL)));
    when(request.formData()).thenReturn(Mono.just(formData));
    when(request.uriBuilder()).thenAnswer(
        invocation -> UriComponentsBuilder.fromUriString("/create-task"));
    var taskToCreate = controller.formTask(formData);
    when(taskStore.createTask(taskToCreate)).thenReturn(Mono.just(taskToCreate.withId(TASK_ID)));

    var response = controller.createTask(request).block();

    assertThat(response)
        .hasStatusCode(SEE_OTHER)
        .hasLocation(URI.create("/"));
  }*/

  @Test
  void formTask() {
    var formData = new LinkedMultiValueMap<>(Map.of("instantiates",
        List.of(ACTIVITY_DEFINITION_URL)));
    when(taskStore.findByUrl(ActivityDefinition.class, ACTIVITY_DEFINITION_URL))
        .thenReturn(Mono.just(ACTIVITY_DEFINITION));
    var taskCreator = mock(TaskCreator.class);
    when(applicationContext.getBean(ACTIVITY_DEFINITION_URL, TaskCreator.class)).thenReturn(
        taskCreator);
    when(taskCreator.create(ACTIVITY_DEFINITION)).thenReturn(Mono.just(TASK));

    var result = controller.formTask(formData);

    StepVerifier.create(result).expectNext(TASK).verifyComplete();
  }
}
