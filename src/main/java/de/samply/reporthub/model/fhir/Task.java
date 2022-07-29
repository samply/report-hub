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
import de.samply.reporthub.model.fhir.Task.Builder;
import de.samply.reporthub.model.fhir.Task.Output.Serializer;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Task(
    Optional<String> id,
    List<Identifier> identifier,
    Optional<String> instantiatesCanonical,
    Code status,
    Optional<OffsetDateTime> lastModified,
    List<Output> output) implements Resource {

  public Task {
    Objects.requireNonNull(id);
    Objects.requireNonNull(identifier);
    Objects.requireNonNull(instantiatesCanonical);
    Objects.requireNonNull(status);
    Objects.requireNonNull(lastModified);
    Objects.requireNonNull(output);
  }

  public Task withId(String id) {
    return new Builder(this).withId(id).build();
  }

  public Optional<String> findIdentifierValue(String system) {
    return identifier.stream()
        .filter(i -> Optional.of(system).equals(i.system()))
        .map(Identifier::value).flatMap(Optional::stream)
        .findFirst();
  }

  public Task withStatus(Code status) {
    return new Builder(this).withStatus(status).build();
  }

  public Task withLastModified(OffsetDateTime lastModified) {
    return new Builder(this).withLastModified(lastModified).build();
  }

  /**
   * Finds the {@link Output} where {@code predicate} matches {@link Output#type}.
   *
   * @param predicate the predicate to use
   * @return an {@code Optional} of the found {@link Output} or an empty {@code Optional} if no
   * Output was found
   */
  public Optional<Output> findOutput(Predicate<CodeableConcept> predicate) {
    return output.stream().filter(output -> predicate.test(output.type)).findFirst();
  }

  public Task addOutput(Output output) {
    List<Output> list = new ArrayList<>(this.output);
    list.add(output);
    return new Builder(this).withOutput(list).build();
  }

  public static Builder builder(Code status) {
    return new Builder(status);
  }

  public static class Builder {

    private String id;
    private List<Identifier> identifier;
    private String instantiatesCanonical;
    private Code status;
    private OffsetDateTime lastModified;
    private List<Output> output;

    public Builder() {
    }

    private Builder(Code status) {
      this.status = Objects.requireNonNull(status);
    }

    private Builder(Task task) {
      id = task.id.orElse(null);
      identifier = task.identifier;
      instantiatesCanonical = task.instantiatesCanonical.orElse(null);
      status = task.status;
      lastModified = task.lastModified.orElse(null);
      output = task.output;
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withIdentifier(List<Identifier> identifier) {
      this.identifier = identifier;
      return this;
    }

    public Builder withInstantiatesCanonical(String instantiatesCanonical) {
      this.instantiatesCanonical = Objects.requireNonNull(instantiatesCanonical);
      return this;
    }

    public Builder withStatus(Code status) {
      this.status = Objects.requireNonNull(status);
      return this;
    }

    public Builder withLastModified(OffsetDateTime lastModified) {
      this.lastModified = Objects.requireNonNull(lastModified);
      return this;
    }

    public Builder withOutput(List<Output> output) {
      this.output = output;
      return this;
    }

    public Task build() {
      return new Task(Optional.ofNullable(id),
          Util.copyOfNullable(identifier),
          Optional.ofNullable(instantiatesCanonical),
          status,
          Optional.ofNullable(lastModified),
          Util.copyOfNullable(output));
    }
  }

  @JsonSerialize(using = Serializer.class)
  @JsonDeserialize(builder = Output.Builder.class)
  public record Output(List<Extension> extension, CodeableConcept type, Element value) implements
      BackboneElement {

    public Output {
      Objects.requireNonNull(extension);
      Objects.requireNonNull(type);
      Objects.requireNonNull(value);
    }

    public Output(CodeableConcept type, Element value) {
      this(List.of(), type, value);
    }

    public <T extends Element> Optional<T> castValue(Class<T> type) {
      return value.cast(type);
    }

    public static Builder builder(CodeableConcept type, Element value) {
      return new Builder(type, value);
    }

    public static class Builder {

      private List<Extension> extension;
      private CodeableConcept type;
      private Element value;

      public Builder() {
      }

      private Builder(CodeableConcept type, Element value) {
        this.type = Objects.requireNonNull(type);
        this.value = Objects.requireNonNull(value);
      }

      public Builder withExtension(List<Extension> extension) {
        this.extension = extension;
        return this;
      }

      public Builder withType(CodeableConcept type) {
        this.type = Objects.requireNonNull(type);
        return this;
      }

      public Builder withValueReference(Reference value) {
        this.value = Objects.requireNonNull(value);
        return this;
      }

      public Output build() {
        return new Output(Util.copyOfNullable(extension), type, value);
      }
    }

    public static class Serializer extends JsonSerializer<Output> {

      @Override
      public void serialize(Output output, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeStartObject();

        gen.writeFieldName("type");
        serializers.findValueSerializer(CodeableConcept.class)
            .serialize(output.type, gen, serializers);

        gen.writeFieldName("value" + output.value.getClass().getSimpleName());
        serializers.findValueSerializer(output.value.getClass())
            .serialize(output.value, gen, serializers);

        gen.writeEndObject();
      }
    }
  }
}
