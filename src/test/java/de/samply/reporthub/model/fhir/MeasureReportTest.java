package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.Util;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class MeasureReportTest {

  @Test
  void deserialize() {
    var measureReport = Util.parseJson("""
        {
          "resourceType": "MeasureReport",
          "status": "draft",
          "type": "individual",
          "measure": "foo"
        }
        """, MeasureReport.class).block();

    assertThat(measureReport).isNotNull();
    assertThat(measureReport.status().value()).contains("draft");
  }

  @Test
  void deserialize_date() {
    var measureReport = Util.parseJson("""
        {
          "resourceType": "MeasureReport",
          "status": "draft",
          "type": "individual",
          "measure": "foo",
          "date": "2022-07-20T21:21:01+02:00"
        }
        """, MeasureReport.class).block();

    assertThat(measureReport).isNotNull();
    assertThat(measureReport.date()).contains(OffsetDateTime.parse("2022-07-20T21:21:01+02:00"));
  }

  @Test
  void serialize_date() {
    var measureReport = MeasureReport.builder(Code.valueOf("draft"), Code.valueOf("individual"),
            "foo")
        .withDate(OffsetDateTime.parse("2022-07-20T21:21:01+02:00"))
        .build();

    var string = Util.prettyPrintJson(measureReport).block();

    assertThat(string).isEqualTo("""
        {
          "resourceType" : "MeasureReport",
          "status" : "draft",
          "type" : "individual",
          "measure" : "foo",
          "date" : "2022-07-20T21:21:01+02:00"
        }""");
  }

  @Test
  void serialize_date_zulu() {
    var measureReport = MeasureReport.builder(Code.valueOf("draft"), Code.valueOf("individual"),
            "foo")
        .withDate(OffsetDateTime.parse("2022-07-20T21:21:01Z"))
        .build();

    var string = Util.prettyPrintJson(measureReport).block();

    assertThat(string).isEqualTo("""
        {
          "resourceType" : "MeasureReport",
          "status" : "draft",
          "type" : "individual",
          "measure" : "foo",
          "date" : "2022-07-20T21:21:01Z"
        }""");
  }
}
