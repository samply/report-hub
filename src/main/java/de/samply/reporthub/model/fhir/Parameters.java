package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Parameters.Builder;
import de.samply.reporthub.model.fhir.Parameters.Parameter.Serializer;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Parameters(
    Optional<String> id,
    Optional<Meta> meta,
    List<Parameter> parameter) implements Resource {

  public Parameters {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(parameter);
  }

  public <T extends Element> Optional<T> findParameterValue(Class<T> type, String name) {
    return parameter.stream().filter(parameter -> name.equals(parameter.name))
        .flatMap(parameter -> parameter.castValue(type).stream())
        .findFirst();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private List<Parameter> parameter;

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withParameter(List<Parameter> parameter) {
      this.parameter = parameter;
      return this;
    }

    public Parameters build() {
      return new Parameters(
          Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Util.copyOfNullable(parameter));
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonSerialize(using = Serializer.class)
  @JsonDeserialize(builder = Parameter.JsonBuilder.class)
  public record Parameter(String name, Optional<Element> value, Optional<Resource> resource) {

    public Parameter {
      Objects.requireNonNull(name);
      Objects.requireNonNull(value);
      Objects.requireNonNull(resource);
    }

    public <T extends Element> Optional<T> castValue(Class<T> type) {
      return value.flatMap(v -> v.cast(type));
    }

    public static Builder builder(String name) {
      return new Builder(name);
    }

    public static class Builder {

      private String name;
      private Element value;
      private Resource resource;

      private Builder(String name) {
        this.name = Objects.requireNonNull(name);
      }

      public Builder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
      }

      public Builder withValue(Element value) {
        this.value = Objects.requireNonNull(value);
        return this;
      }

      public Builder withResource(Resource resource) {
        this.resource = Objects.requireNonNull(resource);
        return this;
      }

      public Parameter build() {
        return new Parameter(name, Optional.ofNullable(value), Optional.ofNullable(resource));
      }
    }

    public static class JsonBuilder {

      private String name;
      private Element value;
      private Resource resource;

      public JsonBuilder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
      }

      public JsonBuilder withValueCanonical(Canonical value) {
        this.value = Objects.requireNonNull(value);
        return this;
      }

      public JsonBuilder withResource(Resource resource) {
        this.resource = Objects.requireNonNull(resource);
        return this;
      }

      public Parameter build() {
        return new Parameter(name, Optional.ofNullable(value), Optional.ofNullable(resource));
      }
    }

    public static class Serializer extends JsonSerializer<Parameter> {

      @Override
      public void serialize(Parameter parameter, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", parameter.name);
        if (parameter.value.isPresent()) {
          var value = parameter.value.get();
          gen.writeFieldName("value" + value.getClass().getSimpleName());
          serializers.findValueSerializer(value.getClass()).serialize(value, gen, serializers);
        }
        if (parameter.resource.isPresent()) {
          var value = parameter.resource.get();
          gen.writeFieldName("resource");
          serializers.findValueSerializer(value.getClass()).serialize(value, gen, serializers);
        }
        gen.writeEndObject();
      }
    }
  }
}
