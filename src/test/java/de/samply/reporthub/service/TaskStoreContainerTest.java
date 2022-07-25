package de.samply.reporthub.service;

import static de.samply.reporthub.model.fhir.PublicationStatus.UNKNOWN;
import static de.samply.reporthub.model.fhir.TaskStatus.DRAFT;
import static de.samply.reporthub.model.fhir.TaskStatus.REQUESTED;
import static de.samply.reporthub.service.TaskStore.BEAM_TASK_ID_SYSTEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.Identifier;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.Task;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TaskStoreContainerTest {

  private static final Logger logger = LoggerFactory.getLogger(TaskStoreContainerTest.class);

  public static final String BEAM_TASK_ID = "6db99ca6-0d0b-4ec9-9657-7b51ca3977fa";
  public static final Identifier BEAM_TASK_IDENTIFIER = Util.beamTaskIdentifier(BEAM_TASK_ID);
  public static final OffsetDateTime DATE_TIME = OffsetDateTime.parse("2022-07-20T21:21:01+02:00");

  @Container
  @SuppressWarnings("resource")
  private final GenericContainer<?> fhirServer = new GenericContainer<>("samply/blaze:0.17")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withEnv("LOG_LEVEL", "debug")
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCode(200))
      .withLogConsumer(new Slf4jLogConsumer(logger));


  private TaskStore taskStore;

  @BeforeEach
  void setUp() {
    WebClient webClient = WebClient.builder()
        .baseUrl("http://%s:%d/fhir".formatted(fhirServer.getHost(), fhirServer.getFirstMappedPort()))
        .defaultRequest(request -> request.accept(APPLICATION_JSON))
        .codecs(configurer -> {
          configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(Util.mapper()));
          configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(Util.mapper()));
        })
        .build();
    taskStore = new TaskStore(webClient);
  }

  @Test
  void createTask_draft() {
    String taskId = UUID.randomUUID().toString();
    var taskToCreate = Task.builder(DRAFT.code())
        .withIdentifier(List.of(Util.beamTaskIdentifier(taskId)))
        .build();

    var task = taskStore.createTask(taskToCreate).block();

    assertNotNull(task);
    assertTrue(task.id().isPresent());
    assertEquals(Optional.of(taskId), task.findIdentifierValue(BEAM_TASK_ID_SYSTEM));
    assertEquals(DRAFT.code(), task.status());
  }

  @Test
  void createTask_requested() {
    String taskId = UUID.randomUUID().toString();
    var taskToCreate = Task.builder(REQUESTED.code())
        .withIdentifier(List.of(Util.beamTaskIdentifier(taskId)))
        .build();

    var task = taskStore.createTask(taskToCreate).block();

    assertNotNull(task);
    assertTrue(task.id().isPresent());
    assertEquals(Optional.of(taskId), task.findIdentifierValue(BEAM_TASK_ID_SYSTEM));
    assertEquals(REQUESTED.code(), task.status());
  }

  /**
   * Tests that tasks with the same Beam ID are created only once.
   */
  @Test
  void createTask_onlyOnce() {
    var taskToCreate = Task.builder(DRAFT.code()).withIdentifier(List.of(BEAM_TASK_IDENTIFIER)).build();
    var existingTaskId = taskStore.createTask(taskToCreate).map(Task::id).block();

    var task = taskStore.createTask(taskToCreate).block();

    assertNotNull(task);
    assertEquals(existingTaskId, task.id());
  }

  @Test
  void listAllActivityDefinitions_empty() {
    var activityDefinitions = taskStore.listAllActivityDefinitions().collectList().block();

    assertNotNull(activityDefinitions);
    assertTrue(activityDefinitions.isEmpty());
  }

  @Test
  void listAllActivityDefinitions_one() {
    String url = UUID.randomUUID().toString();
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl(url).build();
    taskStore.createActivityDefinition(activityDefinitionToCreate).block();

    var activityDefinitions = taskStore.listAllActivityDefinitions().collectList().block();

    assertNotNull(activityDefinitions);
    assertEquals(1, activityDefinitions.size());
  }

  @Test
  void createActivityDefinition() {
    String url = UUID.randomUUID().toString();
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl(url).build();

    var activityDefinition = taskStore.createActivityDefinition(activityDefinitionToCreate).block();

    assertNotNull(activityDefinition);
    assertTrue(activityDefinition.id().isPresent());
    assertEquals(Optional.of(url), activityDefinition.url());
    assertEquals(UNKNOWN.code(), activityDefinition.status());
  }

  @Test
  void createMeasureReport() {
    var measureReportToCreate = MeasureReport.builder(Code.valueOf("draft"),
            Code.valueOf("individual"), "foo")
        .withDate(DATE_TIME)
        .build();

    var measureReport = taskStore.createMeasureReport(measureReportToCreate).block();

    assertNotNull(measureReport);
    assertTrue(measureReport.id().isPresent());
    assertEquals(Code.valueOf("draft"), measureReport.status());
    assertEquals(Code.valueOf("individual"), measureReport.type());
    assertEquals(Optional.of(DATE_TIME), measureReport.date());
  }
}
