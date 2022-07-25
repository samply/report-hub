package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.samply.reporthub.model.fhir.Extension.JsonBuilder;
import de.samply.reporthub.model.fhir.Extension.Serializer;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonSerialize(using = Serializer.class)
@JsonDeserialize(builder = JsonBuilder.class)
public record Extension(String url, Optional<Element> value) implements Element {

  public Extension {
    Objects.requireNonNull(url);
  }

  public <T extends Element> Optional<T> castValue(Class<T> type) {
    return value.flatMap(v -> v.cast(type));
  }

  public static Builder builder(String url) {
    return new Builder(url);
  }

  public static class Builder {

    private final String url;
    private Element value;

    private Builder(String url) {
      this.url = Objects.requireNonNull(url);
    }

    public Builder withValue(Element value) {
      this.value = value;
      return this;
    }

    public Extension build() {
      return new Extension(url, Optional.ofNullable(value));
    }
  }

  public static class JsonBuilder {

    private String url;
    private Element value;

    public JsonBuilder withUrl(String url) {
      this.url = url;
      return this;
    }

    public JsonBuilder withValueCode(Code value) {
      this.value = value;
      return this;
    }

    public Extension build() {
      return new Extension(url, Optional.ofNullable(value));
    }
  }

  public static class Serializer extends JsonSerializer<Extension> {

    @Override
    public void serialize(Extension extension, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeStartObject();
      gen.writeStringField("url", extension.url);
      if (extension.value.isPresent()) {
        var value = extension.value.get();
        gen.writeFieldName("value" + value.getClass().getSimpleName());
        serializers.findValueSerializer(value.getClass()).serialize(value, gen, serializers);
      }
      gen.writeEndObject();
    }
  }
}
