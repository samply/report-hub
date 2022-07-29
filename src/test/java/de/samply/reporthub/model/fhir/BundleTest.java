package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Bundle.Entry.Response;
import org.junit.jupiter.api.Test;

class BundleTest {

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
}
