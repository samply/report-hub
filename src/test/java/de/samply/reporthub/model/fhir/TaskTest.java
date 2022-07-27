package de.samply.reporthub.model.fhir;

import static org.junit.jupiter.api.Assertions.*;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Task.Output;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaskTest {

  @Test
  void deserialize_lastModified() {
    var task = Util.parseJson("""
        {
          "resourceType" : "Task",
          "status" : "draft",
          "lastModified" : "2022-07-25T11:33:19.788694+02:00"
        }""", Task.class).block();

    assertNotNull(task);
    assertEquals(Optional.of(OffsetDateTime.parse("2022-07-25T11:33:19.788694+02:00")), task.lastModified());
  }

  @Test
  void deserialize_outputReference() {
    var task = Util.parseJson("""
        {
          "resourceType" : "Task",
          "status" : "draft",
          "output" : [ {
            "type" : {
              "text" : "foo"
            },
            "valueReference" : {
              "reference" : "bar"
            }
          } ]
        }""", Task.class).block();

    assertNotNull(task);
    assertEquals(Code.valueOf("draft"), task.status());
    assertEquals(Optional.of("bar"), task.output().get(0).castValue(Reference.class).flatMap(Reference::reference));
  }

  @Test
  void serialize_outputReference() {
    var measureReport = Task.builder(TaskStatus.DRAFT.code())
        .withOutput(List.of(Output.builder(
            CodeableConcept.builder().withText("foo").build(),
            Reference.builder().withReference("bar").build()).build()))
        .build();

    assertEquals("""
        {
          "resourceType" : "Task",
          "status" : "draft",
          "output" : [ {
            "type" : {
              "text" : "foo"
            },
            "valueReference" : {
              "reference" : "bar"
            }
          } ]
        }""", Util.prettyPrintJson(measureReport).block());
  }
}
