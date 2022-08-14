package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Task.Output;
import java.time.OffsetDateTime;
import java.util.List;
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

    assertThat(task).isNotNull();
    assertThat(task.lastModified())
        .contains(OffsetDateTime.parse("2022-07-25T11:33:19.788694+02:00"));
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

    assertThat(task).isNotNull();
    assertThat(task.status().value()).contains("draft");
    assertThat(task.output().get(0).castValue(Reference.class).flatMap(Reference::reference))
        .contains("bar");
  }

  @Test
  void serialize_outputReference() {
    var measureReport = Task.draft()
        .withOutput(List.of(Output.builder(
            CodeableConcept.builder().withText("foo").build(),
            Reference.builder().withReference("bar").build()).build()))
        .build();

    var string = Util.prettyPrintJson(measureReport).block();

    assertThat(string).isEqualTo("""
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
        }""");
  }
}
