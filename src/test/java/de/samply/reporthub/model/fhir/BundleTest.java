package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Bundle.Entry.Response;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class BundleTest {

  @Test
  void deserialize_withoutType() {
    var result = Util.parseJson("""
        {"resourceType": "Bundle"}""", Bundle.class);

    StepVerifier.create(result).expectErrorMessage(
            """
                Error while parsing a Bundle: Cannot construct instance of `de.samply.reporthub.model.fhir.Bundle$Builder`, problem: missing type
                 at [Source: (String)"{"resourceType": "Bundle"}"; line: 1, column: 26]""")
        .verify();
  }

  @Test
  void deserialize_typeOnly() {
    var bundle = Util.parseJson("""
        {"resourceType": "Bundle", "type": "transaction"}
        """, Bundle.class).block();

    assertThat(bundle).isNotNull();
    assertThat(bundle.type().value()).contains("transaction");
  }

  @Test
  void deserialize_oneTask() {
    var bundle = Util.parseJson("""
        {
          "resourceType": "Bundle",
          "type": "transaction",
          "entry": [{
            "resource": {
              "resourceType": "Task",
              "status": "accepted"
            }
          }]
        }
        """, Bundle.class).block();

    assertThat(bundle).isNotNull();
    assertThat(bundle.type().value()).contains("transaction");
    assertThat(bundle.resourcesAs(Task.class).findFirst().map(Task::status).flatMap(Code::value))
        .contains("accepted");
  }

  @Test
  void deserialize_oneResponse() {
    var bundle = Util.parseJson("""
        {
          "resourceType": "Bundle",
          "type": "transaction",
          "entry": [{
            "response": {
              "status": "200"
            }
          }]
        }
        """, Bundle.class).block();

    assertThat(bundle).isNotNull();
    assertThat(bundle.type().value()).contains("transaction");
    assertThat(bundle.entry().get(0).response().map(Response::status)).contains("200");
  }

  @Test
  void deserialize_oneMessageHeader() {
    var bundle = Util.parseJson("""
        {
          "resourceType": "Bundle",
          "type": "message",
          "entry": [{
            "resource": {
              "resourceType": "MessageHeader",
              "eventCoding" : {
                "system" : "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
                "code" : "evaluate-measure"
              }
            }
          }]
        }
        """, Bundle.class).block();

    assertThat(bundle).isNotNull();
    assertThat(bundle.type().value()).contains("message");
    assertThat(bundle.resourcesAs(MessageHeader.class).findFirst().map(MessageHeader::eventCoding)
        .flatMap(Coding::code).flatMap(Code::value))
        .contains("evaluate-measure");
  }
}
