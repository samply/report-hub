package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.reporthub.Util;
import org.junit.jupiter.api.Test;

class ExtensionTest {

  @Test
  void deserialize_code() {
    var extension = Util.parseJson("""
        {"url": "foo", "valueCode": "bar"}
        """, Extension.class).block();

    assertThat(extension).isNotNull();
    assertThat(extension.castValue(Code.class).flatMap(Code::value)).contains("bar");
  }

  @Test
  void serialize_code() throws JsonProcessingException {
    var code = Code.builder().withValue("bar").build();
    var extension = Extension.builder("foo").withValue(code).build();

    var string = new ObjectMapper().writeValueAsString(extension);

    assertThat(string).isEqualTo("{\"url\":\"foo\",\"valueCode\":\"bar\"}");
  }
}
