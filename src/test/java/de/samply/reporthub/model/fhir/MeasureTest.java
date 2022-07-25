package de.samply.reporthub.model.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.samply.reporthub.Util;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MeasureTest {

  @Test
  void deserialize() {
    var measure = Util.parseJson("""
        {"resourceType": "Measure", "name": "foo"}
        """, Measure.class).block();

    assertNotNull(measure);
    assertEquals(Optional.of("foo"), measure.name());
  }
}
