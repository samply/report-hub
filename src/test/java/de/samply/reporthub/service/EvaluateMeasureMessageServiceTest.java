package de.samply.reporthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.MessageEvent;
import de.samply.reporthub.model.TaskCode;
import de.samply.reporthub.model.TaskInput;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Bundle.Entry;
import de.samply.reporthub.model.fhir.BundleType;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.Parameters;
import de.samply.reporthub.model.fhir.Parameters.Parameter;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.model.fhir.TaskStatus;
import de.samply.reporthub.model.fhir.Uri;
import java.time.Clock;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class EvaluateMeasureMessageServiceTest {

  private static final String PARAMETERS_URN = "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957";
  private static final Canonical MEASURE_URL = Canonical.valueOf(
      "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard");
  private static final Bundle MESSAGE = Bundle.builder(BundleType.MESSAGE.code())
      .withId("9e1ad660-9fa4-465a-9fca-b21c24c45347")
      .withEntry(List.of(
          Entry.builder()
              .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
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
  private static final Task TASK = Task.builder(TaskStatus.READY.code())
      .withCode(CodeableConcept.of(TaskCode.EVALUATE_MEASURE.coding()))
      .withInput(List.of(Input.of(TaskInput.MEASURE.coding(), MEASURE_URL)))
      .build();
  public static final String TASK_ID = "id-154512";

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
          "id" : "9e1ad660-9fa4-465a-9fca-b21c24c45347",
          "type" : "message",
          "entry" : [ {
            "resource" : {
              "resourceType" : "MessageHeader",
              "eventCoding" : {
                "system" : "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
                "code" : "evaluate-measure"
              },
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
  void processMessage() {
    var acknowledger = new Acknowledger();
    when(taskStore.createTask(TASK)).thenReturn(Mono.just(TASK.withId(TASK_ID)));

    var message = service.processRecord(Record.of(MESSAGE, acknowledger)).block();

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
