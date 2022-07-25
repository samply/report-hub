package de.samply.reporthub.service;

import static de.samply.reporthub.model.fhir.PublicationStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.MeasureReport;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import reactor.test.StepVerifier;

public class TaskStoreMockTest {

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
  void listAllTasks_404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    var activityDefinitions = taskStore.listAllTasks();

    StepVerifier.create(activityDefinitions).expectError(NotFound.class).verify();
  }

  @Test
  void listAllActivityDefinitions_404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    var activityDefinitions = taskStore.listAllActivityDefinitions();

    StepVerifier.create(activityDefinitions).expectError(NotFound.class).verify();
  }

  @Test
  void createActivityDefinition_BadRequest() {
    server.enqueue(new MockResponse().setResponseCode(400)
        .setHeader("Content-Type", "application/fhir+json")
        .setBody("""
        {"resourceType" : "OperationOutcome"}
        """));
    var activityDefinitionToCreate = ActivityDefinition.builder(UNKNOWN.code()).withUrl("").build();

    var measureReport = taskStore.createActivityDefinition(activityDefinitionToCreate);

    StepVerifier.create(measureReport).expectError(BadRequestException.class).verify();
  }

  @Test
  void createMeasureReport_BadRequest() {
    server.enqueue(new MockResponse().setResponseCode(400)
        .setHeader("Content-Type", "application/fhir+json")
        .setBody("""
        {"resourceType" : "OperationOutcome"}
        """));
    var measureReportToCreate = MeasureReport.builder(Code.valueOf("draft"),
            Code.valueOf("individual"), "foo")
        .build();

    var measureReport = taskStore.createMeasureReport(measureReportToCreate);

    StepVerifier.create(measureReport).expectError(BadRequestException.class).verify();
  }
}
