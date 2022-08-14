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
  void deserialize_url() {
    var extension = Util.parseJson("""
        {"url": "foo", "valueUrl": "bar"}
        """, Extension.class).block();

    assertThat(extension).isNotNull();
    assertThat(extension.castValue(Url.class).flatMap(Url::value)).contains("bar");
  }

  @Test
  void deserialize_string() {
    var extension = Util.parseJson("""
        {"url": "foo", "valueString": "bar"}
        """, Extension.class).block();

    assertThat(extension).isNotNull();
    assertThat(extension.castValue(StringElement.class).flatMap(StringElement::value)).contains(
        "bar");
  }

  @Test
  void serialize_code() throws JsonProcessingException {
    var extension = Extension.of("foo", Code.valueOf("bar"));

    var string = new ObjectMapper().writeValueAsString(extension);

    assertThat(string).isEqualTo("{\"url\":\"foo\",\"valueCode\":\"bar\"}");
  }

  @Test
  void serialize_url() throws JsonProcessingException {
    var extension = Extension.of("foo", Url.valueOf("bar"));

    var string = new ObjectMapper().writeValueAsString(extension);

    assertThat(string).isEqualTo("{\"url\":\"foo\",\"valueUrl\":\"bar\"}");
  }

  @Test
  void serialize_string() throws JsonProcessingException {
    var extension = Extension.of("foo", StringElement.valueOf("bar"));

    var string = new ObjectMapper().writeValueAsString(extension);

    assertThat(string).isEqualTo("{\"url\":\"foo\",\"valueString\":\"bar\"}");
  }
}
