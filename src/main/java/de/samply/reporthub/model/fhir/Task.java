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
    Optional<Meta> meta,
    List<Extension> extension,
    List<Identifier> identifier,
    Optional<String> instantiatesCanonical,
    Code status,
    Optional<CodeableConcept> code,
    Optional<OffsetDateTime> lastModified,
    Optional<Restriction> restriction,
    List<Input> input,
    List<Output> output) implements Resource<Task> {

  public Task {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(extension);
    Objects.requireNonNull(identifier);
    Objects.requireNonNull(instantiatesCanonical);
    Objects.requireNonNull(status);
    Objects.requireNonNull(code);
    Objects.requireNonNull(lastModified);
    Objects.requireNonNull(restriction);
    Objects.requireNonNull(input);
    Objects.requireNonNull(output);
  }

  @Override
  public Task withId(String id) {
    return new Builder(this).withId(id).build();
  }

  /**
   * Finds the {@link Extension} with the given {@code url}.
   *
   * @param url the URL to match
   * @return an {@code Optional} of the found {@link Extension} or an empty {@code Optional} if no
   * Extension was found
   */
  public Optional<Extension> findExtension(String url) {
    return extension.stream()
        .filter(e -> Objects.requireNonNull(url).equals(e.url()))
        .findFirst();
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
   * Finds the {@link Input} where {@code predicate} matches its {@link Input#type type}.
   *
   * @param predicate the predicate to use
   * @return an {@code Optional} of the found {@link Input} or an empty {@code Optional} if no Input
   * was found
   */
  public Optional<Input> findInput(Predicate<CodeableConcept> predicate) {
    return input.stream().filter(input -> predicate.test(input.type)).findFirst();
  }

  /**
   * Finds the {@link Output} where {@code predicate} matches its {@link Output#type type}.
   *
   * @param predicate the predicate to use
   * @return an {@code Optional} of the found {@link Output} or an empty {@code Optional} if no
   * Output was found
   */
  public Optional<Output> findOutput(Predicate<CodeableConcept> predicate) {
    return output.stream().filter(output -> predicate.test(output.type)).findFirst();
  }

  public Task addInput(Input input) {
    var list = new ArrayList<>(this.input);
    list.add(input);
    return new Builder(this).withInput(list).build();
  }

  public Task addOutput(Output output) {
    var list = new ArrayList<>(this.output);
    list.add(output);
    return new Builder(this).withOutput(list).build();
  }

  public static Builder draft() {
    return new Builder(TaskStatus.DRAFT.code());
  }

  public static Builder ready() {
    return new Builder(TaskStatus.READY.code());
  }

  public static Builder failed() {
    return new Builder(TaskStatus.FAILED.code());
  }

  public static Builder builder(Code status) {
    return new Builder(status);
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private List<Extension> extension;
    private List<Identifier> identifier;
    private String instantiatesCanonical;
    private Code status;
    private CodeableConcept code;
    private OffsetDateTime lastModified;
    private Restriction restriction;
    private List<Input> input;
    private List<Output> output;

    public Builder() {
    }

    private Builder(Code status) {
      this.status = Objects.requireNonNull(status);
    }

    private Builder(Task task) {
      id = task.id.orElse(null);
      meta = task.meta.orElse(null);
      extension = task.extension;
      identifier = task.identifier;
      instantiatesCanonical = task.instantiatesCanonical.orElse(null);
      status = task.status;
      code = task.code.orElse(null);
      lastModified = task.lastModified.orElse(null);
      restriction = task.restriction.orElse(null);
      input = task.input;
      output = task.output;
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withExtension(List<Extension> extension) {
      this.extension = extension;
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

    public Builder withCode(CodeableConcept code) {
      this.code = Objects.requireNonNull(code);
      return this;
    }

    public Builder withLastModified(OffsetDateTime lastModified) {
      this.lastModified = Objects.requireNonNull(lastModified);
      return this;
    }

    public Builder withRestriction(Restriction restriction) {
      this.restriction = Objects.requireNonNull(restriction);
      return this;
    }

    public Builder withInput(List<Input> input) {
      this.input = input;
      return this;
    }

    public Builder withOutput(List<Output> output) {
      this.output = output;
      return this;
    }

    public Task build() {
      return new Task(Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Util.copyOfNullable(extension),
          Util.copyOfNullable(identifier),
          Optional.ofNullable(instantiatesCanonical),
          status,
          Optional.ofNullable(code),
          Optional.ofNullable(lastModified),
          Optional.ofNullable(restriction),
          Util.copyOfNullable(input),
          Util.copyOfNullable(output));
    }
  }

  @JsonDeserialize(builder = Restriction.Builder.class)
  public record Restriction(List<Reference> recipient) implements BackboneElement {

    public Restriction {
      Objects.requireNonNull(recipient);
    }

    public static class Builder {

      private List<Reference> recipient;

      public Builder withRecipient(List<Reference> recipient) {
        this.recipient = recipient;
        return this;
      }

      public Restriction build() {
        return new Restriction(Util.copyOfNullable(recipient));
      }
    }
  }

  @JsonSerialize(using = Input.Serializer.class)
  @JsonDeserialize(builder = Input.Builder.class)
  public record Input(List<Extension> extension, CodeableConcept type, Element value) implements
      BackboneElement {

    public Input {
      Objects.requireNonNull(extension);
      Objects.requireNonNull(type);
      Objects.requireNonNull(value);
    }

    public static Input of(Coding type, Element value) {
      return new Builder(CodeableConcept.coding(type), value).build();
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

      public Builder withValueString(StringElement value) {
        this.value = Objects.requireNonNull(value);
        return this;
      }

      public Builder withValueReference(Reference value) {
        this.value = Objects.requireNonNull(value);
        return this;
      }

      public Builder withValueCanonical(Canonical value) {
        this.value = Objects.requireNonNull(value);
        return this;
      }

      public Input build() {
        return new Input(Util.copyOfNullable(extension), type, value);
      }
    }

    public static class Serializer extends JsonSerializer<Input> {

      @Override
      public void serialize(Input input, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeStartObject();

        gen.writeFieldName("type");
        serializers.findValueSerializer(CodeableConcept.class)
            .serialize(input.type, gen, serializers);

        gen.writeFieldName("value" + typeName(input.value.getClass()));
        serializers.findValueSerializer(input.value.getClass())
            .serialize(input.value, gen, serializers);

        gen.writeEndObject();
      }

      private static String typeName(Class<?> type) {
        return StringElement.class.equals(type) ? "String" : type.getSimpleName();
      }
    }
  }

  @JsonSerialize(using = Output.Serializer.class)
  @JsonDeserialize(builder = Output.Builder.class)
  public record Output(List<Extension> extension, CodeableConcept type, Element value) implements
      BackboneElement {

    public Output {
      Objects.requireNonNull(extension);
      Objects.requireNonNull(type);
      Objects.requireNonNull(value);
    }

    public static Output of(Coding type, Element value) {
      return new Builder(CodeableConcept.coding(type), value).build();
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

      public Builder withValueString(StringElement value) {
        this.value = Objects.requireNonNull(value);
        return this;
      }

      public Builder withValueCodeableConcept(CodeableConcept value) {
        this.value = Objects.requireNonNull(value);
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

        gen.writeFieldName("value" + typeName(output.value.getClass()));
        serializers.findValueSerializer(output.value.getClass())
            .serialize(output.value, gen, serializers);

        gen.writeEndObject();
      }

      private static String typeName(Class<?> type) {
        return StringElement.class.equals(type) ? "String" : type.getSimpleName();
      }
    }
  }
}
