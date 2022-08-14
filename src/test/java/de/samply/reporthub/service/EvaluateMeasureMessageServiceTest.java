package de.samply.reporthub.service;

import static de.samply.reporthub.service.EvaluateMeasureMessageService.GENERATE_DASHBOARD_REPORT_URL;
import static de.samply.reporthub.service.EvaluateMeasureResponseService.MEASURE_DESTINATION_EXTENSION_URL;
import static de.samply.reporthub.service.EvaluateMeasureResponseService.MEASURE_ID_EXTENSION_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.samply.reporthub.Util;
import de.samply.reporthub.dktk.model.fhir.MessageEvent;
import de.samply.reporthub.dktk.model.fhir.TaskCode;
import de.samply.reporthub.dktk.model.fhir.TaskInput;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Bundle.Entry;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Extension;
import de.samply.reporthub.model.fhir.MeasureReport;
import de.samply.reporthub.model.fhir.MeasureReportStatus;
import de.samply.reporthub.model.fhir.MeasureReportType;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.MessageHeader.Response;
import de.samply.reporthub.model.fhir.MessageHeader.Source;
import de.samply.reporthub.model.fhir.Parameters;
import de.samply.reporthub.model.fhir.Parameters.Parameter;
import de.samply.reporthub.model.fhir.Period;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.ResponseType;
import de.samply.reporthub.model.fhir.StringElement;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.model.fhir.Uri;
import de.samply.reporthub.model.fhir.Url;
import de.samply.reporthub.service.fhir.messaging.MessageBroker;
import de.samply.reporthub.service.fhir.messaging.Record;
import de.samply.reporthub.service.fhir.store.TaskStore;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EvaluateMeasureMessageServiceTest {

  private static final String PARAMETERS_URN = "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957";
  private static final String MEASURE_REPORT_URN = "urn:uuid:224a1318-860b-449c-b20f-03f76fc243fb";
  private static final Canonical MEASURE_URL = Canonical.valueOf(
      "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard");
  private static final Url SOURCE = Url.valueOf("source-174732");
  private static final String MESSAGE_ID = "9e1ad660-9fa4-465a-9fca-b21c24c45347";
  private static final Bundle MESSAGE = Bundle.message()
      .withEntry(List.of(
          Entry.builder()
              .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                  .withId(MESSAGE_ID)
                  .withSource(List.of(Source.endpoint(SOURCE)))
                  .withFocus(List.of(Reference.builder().withReference(PARAMETERS_URN).build()))
                  .build())
              .build(),
          Entry.builder()
              .withFullUrl(Uri.valueOf(PARAMETERS_URN))
              .withResource(Parameters.builder().withParameter(List.of(
                  Parameter.builder("measure").withValue(MEASURE_URL).build()
              )).build())
              .build()
      ))
      .build();
  private static final Bundle RESPONSE_MESSAGE = Bundle.message()
      .withEntry(List.of(
          Entry.builder()
              .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE_RESPONSE.coding())
                  .withId("8c41540d-f832-4bdf-b2a3-f0613714f5e8")
                  .withResponse(Response.of(MESSAGE_ID, ResponseType.OK.code()))
                  .withFocus(List.of(Reference.builder().withReference(MEASURE_REPORT_URN).build()))
                  .build())
              .build(),
          Entry.builder()
              .withFullUrl(Uri.valueOf(MEASURE_REPORT_URN))
              .withResource(MeasureReport.builder(MeasureReportStatus.COMPLETE.code(),
                      MeasureReportType.SUMMARY.code(), MEASURE_URL)
                  .withDate(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
                  .withPeriod(Period.of(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC),
                      OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)))
                  .build())
              .build()
      ))
      .build();
  private static final Task TASK = Task.ready()
      .withExtension(List.of(
          Extension.of(MEASURE_ID_EXTENSION_URL, StringElement.valueOf(MESSAGE_ID)),
          Extension.of(MEASURE_DESTINATION_EXTENSION_URL, SOURCE)
      ))
      .withInstantiatesCanonical(GENERATE_DASHBOARD_REPORT_URL)
      .withCode(CodeableConcept.coding(TaskCode.EVALUATE_MEASURE.coding()))
      .withLastModified(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
      .withInput(List.of(Input.of(TaskInput.MEASURE.coding(), MEASURE_URL)))
      .build();
  private static final String TASK_ID = "id-154512";
  private static final String ERROR_MSG = "error-msg-194654";

  @Mock
  private MessageBroker messageBroker;

  @Mock
  private TaskStore taskStore;

  private EvaluateMeasureMessageService service;

  @BeforeEach
  void setUp() {
    service = new EvaluateMeasureMessageService(messageBroker, taskStore, Clock.fixed(Instant.EPOCH,
        ZoneOffset.UTC));
  }

  @Test
  void printMessage() {
    String message = Util.prettyPrintJson(MESSAGE).block();

    assertThat(message).isEqualTo("""
        {
          "resourceType" : "Bundle",
          "type" : "message",
          "entry" : [ {
            "resource" : {
              "resourceType" : "MessageHeader",
              "id" : "9e1ad660-9fa4-465a-9fca-b21c24c45347",
              "eventCoding" : {
                "system" : "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
                "code" : "evaluate-measure"
              },
              "source" : [ {
                "endpoint" : "source-174732"
              } ],
              "focus" : [ {
                "reference" : "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957"
              } ]
            }
          }, {
            "fullUrl" : "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957",
            "resource" : {
              "resourceType" : "Parameters",
              "parameter" : [ {
                "name" : "measure",
                "valueCanonical" : "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard"
              } ]
            }
          } ]
        }""");
  }

  @Test
  void printResponseMessage() {
    String message = Util.prettyPrintJson(RESPONSE_MESSAGE).block();

    assertThat(message).isEqualTo("""
        {
          "resourceType" : "Bundle",
          "type" : "message",
          "entry" : [ {
            "resource" : {
              "resourceType" : "MessageHeader",
              "id" : "8c41540d-f832-4bdf-b2a3-f0613714f5e8",
              "eventCoding" : {
                "system" : "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
                "code" : "evaluate-measure-response"
              },
              "response" : {
                "identifier" : "9e1ad660-9fa4-465a-9fca-b21c24c45347",
                "code" : "ok"
              },
              "focus" : [ {
                "reference" : "urn:uuid:224a1318-860b-449c-b20f-03f76fc243fb"
              } ]
            }
          }, {
            "fullUrl" : "urn:uuid:224a1318-860b-449c-b20f-03f76fc243fb",
            "resource" : {
              "resourceType" : "MeasureReport",
              "status" : "complete",
              "type" : "summary",
              "measure" : "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard",
              "date" : "1970-01-01T00:00:00Z",
              "period" : {
                "start" : "1970-01-01T00:00:00Z",
                "end" : "1970-01-01T00:00:00Z"
              }
            }
          } ]
        }""");
  }

  @Test
  void task_transactionBundle() {
    var result = service.task(Bundle.transaction().build());

    StepVerifier.create(result)
        .expectErrorMessage("Expect Bundle type to be `message` but was `transaction`.")
        .verify();
  }

  @Test
  void task_messageWithoutHeader() {
    var result = service.task(Bundle.message().build());

    StepVerifier.create(result)
        .expectErrorMessage("Message header expected.")
        .verify();
  }

  @Test
  void task_messageWithoutId() {
    var result = service.task(Bundle.message()
        .withEntry(List.of(Entry.builder()
            .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                .build())
            .build()))
        .build());

    StepVerifier.create(result)
        .expectErrorMessage("Message id expected.")
        .verify();
  }

  @Test
  void task_messageWithoutSource() {
    var result = service.task(Bundle.message()
        .withEntry(List.of(Entry.builder()
            .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                .withId("0d0a95cf-b604-451e-a195-8fa987e10f4f")
                .build())
            .build()))
        .build());

    StepVerifier.create(result)
        .expectErrorMessage("Message source expected.")
        .verify();
  }

  @Test
  void task_messageWithoutParameters() {
    var result = service.task(Bundle.message()
        .withEntry(List.of(Entry.builder()
            .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                .withId("0d0a95cf-b604-451e-a195-8fa987e10f4f")
                .withSource(List.of(Source.endpoint(SOURCE)))
                .build())
            .build()))
        .build());

    StepVerifier.create(result)
        .expectErrorMessage("Parameters resource in focus expected.")
        .verify();
  }

  @Test
  void task_messageWithoutResolvableParameters() {
    var result = service.task(Bundle.message()
        .withEntry(List.of(Entry.builder()
            .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                .withId("0d0a95cf-b604-451e-a195-8fa987e10f4f")
                .withSource(List.of(Source.endpoint(SOURCE)))
                .withFocus(List.of(Reference.builder().withReference(PARAMETERS_URN).build()))
                .build())
            .build()))
        .build());

    StepVerifier.create(result)
        .expectErrorMessage("Parameters resource in focus expected.")
        .verify();
  }

  @Test
  void task_messageWithoutMeasure() {
    var result = service.task(Bundle.message()
        .withEntry(List.of(
            Entry.builder()
                .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                    .withId(MESSAGE_ID)
                    .withSource(List.of(Source.endpoint(SOURCE)))
                    .withFocus(List.of(Reference.builder().withReference(PARAMETERS_URN).build()))
                    .build())
                .build(),
            Entry.builder()
                .withFullUrl(Uri.valueOf(PARAMETERS_URN))
                .withResource(Parameters.builder().build())
                .build()
        ))
        .build());

    StepVerifier.create(result)
        .expectErrorMessage("Missing parameter with name `measure` and type Canonical.")
        .verify();
  }

  /**
   * Tests that invalid messages are skipped and the record is acknowledged.
   */
  @Test
  void processMessage_invalidMessage() {
    var acknowledger = new Acknowledger();

    var result = service.processRecord(Record.of(Bundle.message().build(), acknowledger));

    StepVerifier.create(result).verifyComplete();
    assertThat(acknowledger.isAcknowledged()).isTrue();
  }

  /**
   * Tests that task creation errors bubble up and don't acknowledge the record.
   */
  @Test
  void processMessage_createTaskError() {
    var acknowledger = new Acknowledger();
    when(taskStore.createTask(TASK)).thenReturn(Mono.error(new Exception(ERROR_MSG)));

    var result = service.processRecord(Record.of(MESSAGE, acknowledger));

    StepVerifier.create(result).expectErrorMessage(ERROR_MSG).verify();
    assertThat(acknowledger.isAcknowledged()).isFalse();
  }

  @Test
  void processMessage() {
    var acknowledger = new Acknowledger();
    when(taskStore.createTask(TASK)).thenReturn(Mono.just(TASK.withId(TASK_ID)));

    var result = service.processRecord(Record.of(MESSAGE, acknowledger));

    StepVerifier.create(result).expectNext(MESSAGE).verifyComplete();
    assertThat(acknowledger.isAcknowledged()).isTrue();
  }

  private static class Acknowledger implements Supplier<Mono<Void>> {

    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    @Override
    public Mono<Void> get() {
      acknowledged.set(true);
      return Mono.empty();
    }

    private boolean isAcknowledged() {
      return acknowledged.get();
    }
  }
}
