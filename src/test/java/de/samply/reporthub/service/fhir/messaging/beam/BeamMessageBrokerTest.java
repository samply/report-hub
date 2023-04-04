package de.samply.reporthub.service.fhir.messaging.beam;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.samply.reporthub.Util;
import de.samply.reporthub.dktk.model.fhir.MessageEvent;
import de.samply.reporthub.model.beam.BeamResult;
import de.samply.reporthub.model.beam.BeamTask;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Bundle.Entry;
import de.samply.reporthub.model.fhir.MessageHeader;
import de.samply.reporthub.model.fhir.MessageHeader.Destination;
import de.samply.reporthub.model.fhir.MessageHeader.Response;
import de.samply.reporthub.model.fhir.MessageHeader.Source;
import de.samply.reporthub.model.fhir.Organization;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.ResponseType;
import de.samply.reporthub.model.fhir.Uri;
import de.samply.reporthub.model.fhir.Url;
import de.samply.reporthub.service.beam.BeamTaskBroker;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BeamMessageBrokerTest {

  private static final String APP_ID = "app-id-150420";
  private static final UUID TASK_ID = UUID.fromString("b31e7b08-66ef-4737-aab0-da9c4ec88f6c");
  private static final String ERROR_MSG = "error-msg-130508";
  private static final String VALID_TASK_BODY = """
      {
        "resourceType": "Bundle",
        "type": "message",
        "entry" : [{
          "resource": {
            "resourceType": "MessageHeader",
            "eventCoding" : {
              "system" : "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
              "code" : "evaluate-measure"
            }
          }
        }]
      }
      """;
  private static final String DESTINATION = "destination-151625";
  private static final UUID MESSAGE_ID = UUID.fromString("49a3932d-c79c-4a64-9088-6c08c9ee31c9");
  private static final UUID ORIGINAL_MESSAGE_ID =
      UUID.fromString("2233026d-c573-4a4e-a06a-a2bf7d6945d4");
  private static final Bundle MESSAGE = Bundle.message()
      .withEntry(List.of(
          Entry.builder()
              .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                  .withId(MESSAGE_ID.toString())
                  .withDestination(List.of(Destination.endpoint(Url.valueOf(DESTINATION))))
                  .build())
              .build()
      ))
      .build();
  private static final Bundle RESPONSE_MESSAGE_NO_FOCUS = Bundle.message()
      .withEntry(List.of(
          Entry.builder()
              .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                  .withId(MESSAGE_ID.toString())
                  .withDestination(List.of(Destination.endpoint(Url.valueOf(DESTINATION))))
                  .withResponse(Response.of(ORIGINAL_MESSAGE_ID.toString(), ResponseType.OK.code()))
                  .build())
              .build()
      ))
      .build();
  private static final String FOCUS_URN = "urn:uuid:e01ab47c-fc44-4758-88a1-554c2117c64f";
  public static final Organization FOCUS_RESOURCE = Organization.builder().build();
  private static final Bundle RESPONSE_MESSAGE = Bundle.message()
      .withEntry(List.of(
          Entry.builder()
              .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                  .withId(MESSAGE_ID.toString())
                  .withDestination(List.of(Destination.endpoint(Url.valueOf(DESTINATION))))
                  .withResponse(Response.of(ORIGINAL_MESSAGE_ID.toString(), ResponseType.OK.code()))
                  .withFocus(List.of(Reference.ofReference(FOCUS_URN)))
                  .build())
              .build(),
          Entry.builder()
              .withFullUrl(Uri.valueOf(FOCUS_URN))
              .withResource(FOCUS_RESOURCE)
              .build()
      ))
      .build();

  @Mock
  private BeamTaskBroker client;

  private BeamMessageBroker broker;

  @BeforeEach
  void setUp() {
    broker = new BeamMessageBroker(APP_ID, client);
  }

  @Test
  void send() {
    var body = base64Encode(Objects.requireNonNull(Util.printJson(MESSAGE).block()));
    var task = BeamTask.of(MESSAGE_ID, APP_ID, List.of(DESTINATION), "1h", body);
    when(client.createTask(task)).thenReturn(Mono.empty());

    var result = broker.send(MESSAGE);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void send_responseWithoutFocus() {
    var result = broker.send(RESPONSE_MESSAGE_NO_FOCUS);

    StepVerifier.create(result).expectErrorMessage("Missing resource in focus.").verify();
  }

  @Test
  void send_response() {
    var body = Objects.requireNonNull(Util.printJson(FOCUS_RESOURCE).block());
    var beamResult = BeamResult.base64Succeeded(APP_ID, List.of(DESTINATION), ORIGINAL_MESSAGE_ID,
        body);
    when(client.answerTask(beamResult)).thenReturn(Mono.empty());

    var result = broker.send(RESPONSE_MESSAGE);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void receive_noTasks() {
    when(client.retrieveTasks()).thenReturn(Flux.empty());

    var result = broker.receive(message -> true);

    StepVerifier.create(result).verifyComplete();
  }

  /**
   * Tests that tasks with invalid bodies will be skipped and claimed, so that we don't see them
   * again.
   */
  @ParameterizedTest
  @ValueSource(strings = {"{}", """
      {"resourceType": "Bundle", "type": "transaction"}
      """, """
      {"resourceType": "Bundle", "type": "message"}
      """})
  void receive_oneTaskWithInvalidBody(String decodedBody) {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", base64Encode(decodedBody));
    when(client.retrieveTasks()).thenReturn(Flux.just(task));
    when(client.claimTask(task)).thenReturn(Mono.empty());

    var result = broker.receive(message -> true);

    StepVerifier.create(result).verifyComplete();
  }

  /**
   * Tests errors on claiming an invalid task will bubble up.
   */
  @Test
  void receive_oneTaskWithInvalidBodyAndClaimError() {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", base64Encode("{}"));
    when(client.retrieveTasks()).thenReturn(Flux.just(task));
    when(client.claimTask(task)).thenReturn(Mono.error(new Exception(ERROR_MSG)));

    var result = broker.receive(message -> true);

    StepVerifier.create(result).expectErrorMessage(ERROR_MSG).verify();
  }

  /**
   * Tests that a valid but non-matching task will be skipped and claimed, so that we don't see it
   * again.
   */
  @Test
  void receive_oneNonMatchingTask() {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", base64Encode(VALID_TASK_BODY));
    when(client.retrieveTasks()).thenReturn(Flux.just(task));
    when(client.claimTask(task)).thenReturn(Mono.empty());

    var result = broker.receive(message -> false);

    StepVerifier.create(result).verifyComplete();
  }

  /**
   * Tests errors on claiming a valid task will bubble up.
   */
  @Test
  void receive_oneNonMatchingTaskAndClaimError() {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", base64Encode(VALID_TASK_BODY));
    when(client.retrieveTasks()).thenReturn(Flux.just(task));
    when(client.claimTask(task)).thenReturn(Mono.error(new Exception(ERROR_MSG)));

    var result = broker.receive(message -> false);

    StepVerifier.create(result).expectErrorMessage(ERROR_MSG).verify();
  }

  @Test
  void receive_oneTask() {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", base64Encode(VALID_TASK_BODY));
    when(client.retrieveTasks()).thenReturn(Flux.just(task));

    var result = broker.receive(message -> true);

    StepVerifier.create(result)
        .expectNextMatches(record -> Bundle.message()
            .withEntry(List.of(Entry.builder()
                .withResource(MessageHeader.builder(MessageEvent.EVALUATE_MEASURE.coding())
                    .withId(TASK_ID.toString())
                    .withSource(List.of(Source.endpoint(Url.valueOf(APP_ID))))
                    .build())
                .build()))
            .build().equals(record.message()))
        .verifyComplete();
  }

  @Test
  void receive_oneTaskAndAcknowledgeIt() {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", base64Encode(VALID_TASK_BODY));
    when(client.retrieveTasks()).thenReturn(Flux.just(task));
    var record = broker.receive(message -> true).blockFirst();
    when(client.claimTask(task)).thenReturn(Mono.empty());

    var result = Objects.requireNonNull(record).acknowledge();

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void validateMessage_unexpectedBundleType() {
    var message = Bundle.transaction().build();

    var result = BeamMessageBroker.validateMessage(message);

    StepVerifier.create(result)
        .expectErrorMessage("Expect Bundle type to be `message` but was `transaction`.")
        .verify();
  }

  @Test
  void validateMessage_missingHeader() {
    var message = Bundle.message().build();

    var result = BeamMessageBroker.validateMessage(message);

    StepVerifier.create(result)
        .expectErrorMessage("Missing message header.")
        .verify();
  }

  /**
   * Tests that {@code parseBody} returns an empty {@link Mono} on different invalid inputs:
   * <p>
   * <ul>
   *   <li>{@code "a"} - invalid base64</li>
   *   <li>{@code "YQo="} - invalid JSON (an {@code "a"})</li>
   *   <li>{@code "e30K"} - invalid Bundle (a {@code "{}"})</li>
   * </ul>
   *
   * @param body the body to use
   */
  @ParameterizedTest
  @CsvSource({
      "a,Error while decoding base64 encoded task body:",
      "YQo=,Error while parsing a Bundle:",
      "e30K,Error while parsing a Bundle:"})
  void parseBody_completeOnInvalid(String body, String errorMessagePrefix) {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", body);

    var result = broker.parseBody(task);

    StepVerifier.create(result)
        .expectErrorSatisfies(e -> assertThat(e).message().startsWith(errorMessagePrefix))
        .verify();
  }

  @Test
  void parseBody_success() {
    var task = BeamTask.of(TASK_ID, APP_ID, List.of(APP_ID), "1h", base64Encode("""
        {"resourceType": "Bundle", "type": "message"}
        """));

    var result = broker.parseBody(task);

    StepVerifier.create(result)
        .expectNext(Bundle.message().build())
        .verifyComplete();
  }

  @Test
  void base64Decode() {
    var result = BeamMessageBroker.base64Decode("a");

    StepVerifier.create(result)
        .expectErrorMessage("Error while decoding base64 encoded task body: Input byte[] should at "
                            + "least have 2 bytes for base64 bytes")
        .verify();
  }

  private static String base64Encode(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes(UTF_8));
  }
}
