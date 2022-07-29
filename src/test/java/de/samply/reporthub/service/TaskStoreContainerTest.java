package de.samply.reporthub.service;

import static de.samply.reporthub.model.fhir.PublicationStatus.UNKNOWN;
import static de.samply.reporthub.model.fhir.TaskStatus.DRAFT;
import static de.samply.reporthub.model.fhir.TaskStatus.REQUESTED;
import static de.samply.reporthub.service.TaskStore.BEAM_TASK_ID_SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.CapabilityStatement.Software;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.Identifier;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.Task;
import java.time.OffsetDateTime;
import java.util.List;
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

  private static final String BEAM_TASK_ID = "6db99ca6-0d0b-4ec9-9657-7b51ca3977fa";
  private static final Identifier BEAM_TASK_IDENTIFIER = Util.beamTaskIdentifier(BEAM_TASK_ID);
  private static final OffsetDateTime DATE_TIME = OffsetDateTime.parse("2022-07-20T21:21:01+02:00");

  @Container
  @SuppressWarnings("resource")
  private final GenericContainer<?> blaze = new GenericContainer<>("samply/blaze:0.17")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withEnv("LOG_LEVEL", "debug")
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCode(200))
      .withLogConsumer(new Slf4jLogConsumer(logger));


  private TaskStore taskStore;

  @SuppressWarnings("HttpUrlsUsage")
  @BeforeEach
  void setUp() {
    WebClient webClient = WebClient.builder()
        .baseUrl("http://%s:%d/fhir".formatted(blaze.getHost(), blaze.getFirstMappedPort()))
        .defaultRequest(request -> request.accept(APPLICATION_JSON))
        .codecs(configurer -> {
          configurer.defaultCodecs().maxInMemorySize(1024 * 1024);
          configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(Util.mapper()));
          configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(Util.mapper()));
        })
        .build();
    taskStore = new TaskStore(webClient);
  }

  @Test
  void fetchMetadata() {
    var capabilityStatement = taskStore.fetchMetadata().block();

    assertThat(capabilityStatement).isNotNull();
    assertThat(capabilityStatement.software().map(Software::name)).contains("Blaze");
  }

  @Test
  void fetchTask() {
    var taskToCreate = Task.builder(DRAFT.code()).build();
    var id = taskStore.createTask(taskToCreate).blockOptional().flatMap(Task::id).orElseThrow();

    var task = taskStore.fetchTask(id).block();

    assertThat(task).isNotNull();
    assertThat(task.status().value()).as("task status").contains("draft");
  }

  @Test
  void createTask_draft() {
    var taskId = UUID.randomUUID().toString();
    var taskToCreate = Task.builder(DRAFT.code())
        .withIdentifier(List.of(Util.beamTaskIdentifier(taskId)))
        .build();

    var task = taskStore.createBeamTask(taskToCreate).block();

    assertThat(task).isNotNull();
    assertThat(task.id()).isPresent();
    assertThat(task.findIdentifierValue(BEAM_TASK_ID_SYSTEM)).contains(taskId);
    assertThat(task.status().value()).as("task status").contains("draft");
  }

  @Test
  void createTask_requested() {
    var beamId = UUID.randomUUID().toString();
    var taskToCreate = Task.builder(REQUESTED.code())
        .withIdentifier(List.of(Util.beamTaskIdentifier(beamId)))
        .build();

    var task = taskStore.createBeamTask(taskToCreate).block();

    assertThat(task).isNotNull();
    assertThat(task.id()).isPresent();
    assertThat(task.findIdentifierValue(BEAM_TASK_ID_SYSTEM)).contains(beamId);
    assertThat(task.status().value()).as("task status").contains("requested");
  }

  /**
   * Tests that tasks with the same Beam ID are created only once.
   */
  @Test
  void createTask_onlyOnce() {
    var taskToCreate = Task.builder(DRAFT.code()).withIdentifier(List.of(BEAM_TASK_IDENTIFIER))
        .build();
    var existingTaskId = taskStore.createBeamTask(taskToCreate).blockOptional().flatMap(Task::id)
        .orElseThrow();

    var task = taskStore.createBeamTask(taskToCreate).block();

    assertThat(task).isNotNull();
    assertThat(task.id()).contains(existingTaskId);
  }

  @Test
  void listAllActivityDefinitions_empty() {
    var activityDefinitions = taskStore.listAllActivityDefinitions().collectList().block();

    assertThat(activityDefinitions).isEmpty();
  }

  @Test
  void listAllActivityDefinitions_one() {
    var url = UUID.randomUUID().toString();
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl(url)
        .build();
    taskStore.createActivityDefinition(activityDefinitionToCreate).block();

    var activityDefinitions = taskStore.listAllActivityDefinitions().collectList().block();

    assertThat(activityDefinitions).hasSize(1);
  }

  @Test
  void createActivityDefinition() {
    String url = UUID.randomUUID().toString();
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl(url)
        .build();

    var activityDefinition = taskStore.createActivityDefinition(activityDefinitionToCreate).block();

    assertThat(activityDefinition).isNotNull();
    assertThat(activityDefinition.id()).isPresent();
    assertThat(activityDefinition.url()).contains(url);
    assertThat(activityDefinition.status().value()).contains("unknown");
  }

  /**
   * Tests that activity definitions with the same canonical URLs are created only once.
   */
  @Test
  void createActivityDefinition_onlyOnce() {
    var url = UUID.randomUUID().toString();
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl(url)
        .build();
    var existingId = taskStore.createActivityDefinition(activityDefinitionToCreate).blockOptional()
        .flatMap(ActivityDefinition::id).orElseThrow();

    var activityDefinition = taskStore.createActivityDefinition(activityDefinitionToCreate).block();

    assertThat(activityDefinition).isNotNull();
    assertThat(activityDefinition.id()).contains(existingId);
  }

  @Test
  void createMeasureReport() {
    var measureReportToCreate = MeasureReport.builder(Code.valueOf("draft"),
            Code.valueOf("individual"), "foo")
        .withDate(DATE_TIME)
        .build();

    var measureReport = taskStore.createMeasureReport(measureReportToCreate).block();

    assertThat(measureReport).isNotNull();
    assertThat(measureReport.id()).isPresent();
    assertThat(measureReport.status().value()).contains("draft");
    assertThat(measureReport.type().value()).contains("individual");
    assertThat(measureReport.date()).contains(DATE_TIME);
  }
}
