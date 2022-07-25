package de.samply.reporthub.model.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.samply.reporthub.Util;
import java.time.OffsetDateTime;
import java.util.Optional;
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

    assertNotNull(measureReport);
    assertEquals(Code.valueOf("draft"), measureReport.status());
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

    assertNotNull(measureReport);
    assertEquals(Optional.of(OffsetDateTime.parse("2022-07-20T21:21:01+02:00")),
        measureReport.date());
  }

  @Test
  void serialize_date() {
    var measureReport = MeasureReport.builder(Code.valueOf("draft"), Code.valueOf("individual"), "foo")
        .withDate(OffsetDateTime.parse("2022-07-20T21:21:01+02:00"))
        .build();

    assertEquals("""
        {
          "resourceType" : "MeasureReport",
          "status" : "draft",
          "type" : "individual",
          "measure" : "foo",
          "date" : "2022-07-20T21:21:01+02:00"
        }""", Util.prettyPrintJson(measureReport).block());
  }

  @Test
  void serialize_date_zulu() {
    var measureReport = MeasureReport.builder(Code.valueOf("draft"), Code.valueOf("individual"), "foo")
        .withDate(OffsetDateTime.parse("2022-07-20T21:21:01Z"))
        .build();

    assertEquals("""
        {
          "resourceType" : "MeasureReport",
          "status" : "draft",
          "type" : "individual",
          "measure" : "foo",
          "date" : "2022-07-20T21:21:01Z"
        }""", Util.prettyPrintJson(measureReport).block());
  }
}
