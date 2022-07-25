package de.samply.reporthub.model.fhir;

import static de.samply.reporthub.model.fhir.BundleType.TRANSACTION;
import static de.samply.reporthub.model.fhir.TaskStatus.ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Bundle.Entry.Response;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BundleTest {

  @Test
  void deserialize_typeOnly() {
    var bundle = Util.parseJson("""
        {"resourceType": "Bundle", "type": "transaction"}
        """, Bundle.class).block();

    assertNotNull(bundle);
    assertEquals(TRANSACTION.code(), bundle.type());
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
        """, new TypeReference<Bundle>() {
    }).block();

    assertNotNull(bundle);
    assertEquals(TRANSACTION.code(), bundle.type());
    assertEquals(Optional.of(ACCEPTED.code()), bundle.resourcesAs(Task.class).findFirst().map(Task::status));
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
        """, new TypeReference<Bundle>() {
    }).block();

    assertNotNull(bundle);
    assertEquals(TRANSACTION.code(), bundle.type());
    assertEquals(Optional.of("200"), bundle.entry().get(0).response().map(Response::status));
  }
}
