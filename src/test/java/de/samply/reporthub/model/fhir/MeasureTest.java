package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.Util;
import org.junit.jupiter.api.Test;

class MeasureTest {

  @Test
  void deserialize() {
    var measure = Util.parseJson("""
        {"resourceType": "Measure", "status": "draft"}
        """, Measure.class).block();

    assertThat(measure).isNotNull();
    assertThat(measure.status().value()).contains("draft");
  }
}
