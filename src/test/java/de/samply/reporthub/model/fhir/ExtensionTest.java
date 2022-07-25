package de.samply.reporthub.model.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExtensionTest {

  @Test
  void deserialize_code() throws JsonProcessingException {
    var extension = new ObjectMapper().readValue("{\"url\": \"foo\", \"valueCode\": \"bar\"}",
        Extension.class);

    assertEquals(Optional.of("bar"), extension.castValue(Code.class).flatMap(Code::value));
  }

  @Test
  void serialize_code() throws JsonProcessingException {
    var code = Code.builder().withValue("bar").build();
    var extension = Extension.builder("foo").withValue(code).build();

    var string = new ObjectMapper().writeValueAsString(extension);

    assertEquals("{\"url\":\"foo\",\"valueCode\":\"bar\"}", string);
  }
}
