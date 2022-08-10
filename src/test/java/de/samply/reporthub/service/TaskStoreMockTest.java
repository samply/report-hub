package de.samply.reporthub.service;

import static de.samply.reporthub.model.fhir.PublicationStatus.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.MeasureReportStatus;
import de.samply.reporthub.model.fhir.OperationOutcome;
import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError;
import reactor.test.StepVerifier;

public class TaskStoreMockTest {

  private static final String TASK_ID = "id-122638";
  private static final String MEASURE_REPORT_ID = "id-165829";
  private static final String OPERATION_OUTCOME_ID = "id-162812";

  private MockWebServer server;

  private TaskStore taskStore;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    taskStore = new TaskStore(WebClient.create("http://localhost:%d".formatted(server.getPort())));
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void fetchMetadata_500() {
    server.enqueue(new MockResponse().setResponseCode(500));

    var result = taskStore.fetchMetadata();

    StepVerifier.create(result).expectError(InternalServerError.class).verify();
  }

  @Test
  void fetchTask_404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    var result = taskStore.fetchTask(TASK_ID);

    StepVerifier.create(result)
        .expectErrorSatisfies(new ResourceNotFoundAssert("Task", TASK_ID))
        .verify();
  }

  @Test
  void listAllTasks_404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    var result = taskStore.listNewestTasks();

    StepVerifier.create(result)
        .expectErrorSatisfies(new NotFoundAssert("Task endpoint not found"))
        .verify();
  }

  @Test
  void listAllTasks_connectionRefused() throws IOException {
    server.shutdown();

    var result = taskStore.listNewestTasks();

    StepVerifier.create(result).expectError(WebClientRequestException.class).verify();
  }

  @Test
  void listAllActivityDefinitions_404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    var result = taskStore.listAllActivityDefinitions();

    StepVerifier.create(result)
        .expectErrorSatisfies(new NotFoundAssert("ActivityDefinition endpoint not found"))
        .verify();
  }

  @Test
  void createActivityDefinition_BadRequest() {
    server.enqueue(new MockResponse().setResponseCode(400));
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl("").build();

    var result = taskStore.createActivityDefinition(activityDefinitionToCreate);

    StepVerifier.create(result).expectError(BadRequestException.class).verify();
  }

  @Test
  void createActivityDefinition_BadRequestWithOperationOutcome() {
    server.enqueue(new MockResponse().setResponseCode(400)
        .setHeader("Content-Type", "application/fhir+json")
        .setBody("""
            {"resourceType" : "OperationOutcome",
             "id" : "%s"}
            """.formatted(OPERATION_OUTCOME_ID)));
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl("").build();

    var result = taskStore.createActivityDefinition(activityDefinitionToCreate);

    StepVerifier.create(result)
        .expectErrorSatisfies(new BadRequestAssert(operationOutcome ->
            assertThat(operationOutcome.id()).contains(OPERATION_OUTCOME_ID)))
        .verify();
  }

  @Test
  void fetchMeasureReport_404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    var result = taskStore.fetchMeasureReport(MEASURE_REPORT_ID);

    StepVerifier.create(result)
        .expectErrorSatisfies(new ResourceNotFoundAssert("MeasureReport", MEASURE_REPORT_ID))
        .verify();
  }

  @Test
  void createMeasureReport_BadRequest() {
    server.enqueue(new MockResponse().setResponseCode(400)
        .setHeader("Content-Type", "application/fhir+json")
        .setBody("""
            {"resourceType" : "OperationOutcome"}
            """));
    var measureReportToCreate = MeasureReport.builder(MeasureReportStatus.COMPLETE.code(),
            Code.valueOf("individual"), Canonical.valueOf("foo"))
        .build();

    var result = taskStore.createMeasureReport(measureReportToCreate);

    StepVerifier.create(result).expectError(BadRequestException.class).verify();
  }

  record BadRequestAssert(Consumer<OperationOutcome> operationOutcomeRequirements) implements
      Consumer<Throwable> {

    @Override
    public void accept(Throwable t) {
      assertThat(t).isInstanceOfSatisfying(BadRequestException.class,
          e -> assertThat(e.getOperationOutcome()).as("OperationOutcome")
              .satisfies(operationOutcomeRequirements));
    }
  }

  record NotFoundAssert(String message) implements Consumer<Throwable> {

    @Override
    public void accept(Throwable t) {
      assertThat(t).isInstanceOfSatisfying(NotFoundException.class,
          e -> assertThat(e.getMessage()).as("message").isEqualTo(message));
    }
  }

  record ResourceNotFoundAssert(String type, String id) implements Consumer<Throwable> {

    @Override
    public void accept(Throwable t) {
      assertThat(t).isInstanceOfSatisfying(ResourceNotFoundException.class, e -> {
        assertThat(e.getType()).as("type").isEqualTo(type);
        assertThat(e.getId()).as("id").isEqualTo(id);
      });
    }
  }
}
