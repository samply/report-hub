package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.MessageHeader.Builder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record MessageHeader(
    Optional<String> id,
    Optional<Meta> meta,
    Coding eventCoding,
    List<Destination> destination,
    List<Source> source,
    Optional<Response> response,
    List<Reference> focus) implements Resource<MessageHeader> {

  public MessageHeader {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(eventCoding);
    Objects.requireNonNull(destination);
    Objects.requireNonNull(source);
    Objects.requireNonNull(response);
    Objects.requireNonNull(focus);
  }

  @Override
  public MessageHeader withId(String id) {
    return new Builder(this).withId(id).build();
  }

  public MessageHeader withDestination(List<Destination> destination) {
    return new Builder(this).withDestination(destination).build();
  }

  public MessageHeader withSource(List<Source> source) {
    return new Builder(this).withSource(source).build();
  }

  public Optional<Reference> findFirstFocus() {
    return focus.stream().findFirst();
  }

  public static Predicate<MessageHeader> hasEventCoding(Predicate<Coding> predicate) {
    return messageHeader -> predicate.test(messageHeader.eventCoding);
  }

  public static Builder builder(Coding eventCoding) {
    return new Builder(eventCoding);
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private Coding eventCoding;
    private List<Destination> destination;
    private List<Source> source;
    private Response response;
    private List<Reference> focus;

    public Builder() {
    }

    private Builder(Coding eventCoding) {
      this.eventCoding = Objects.requireNonNull(eventCoding);
    }

    private Builder(MessageHeader header) {
      id = header.id.orElse(null);
      meta = header.meta.orElse(null);
      eventCoding = header.eventCoding;
      destination = header.destination;
      source = header.source;
      response = header.response.orElse(null);
      focus = header.focus;
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withEventCoding(Coding eventCoding) {
      this.eventCoding = Objects.requireNonNull(eventCoding);
      return this;
    }

    public Builder withDestination(List<Destination> destination) {
      this.destination = destination;
      return this;
    }

    public Builder withSource(List<Source> source) {
      this.source = source;
      return this;
    }

    public Builder withResponse(Response response) {
      this.response = Objects.requireNonNull(response);
      return this;
    }

    public Builder withFocus(List<Reference> focus) {
      this.focus = focus;
      return this;
    }

    public MessageHeader build() {
      return new MessageHeader(
          Optional.ofNullable(id),
          Optional.ofNullable(meta),
          eventCoding,
          Util.copyOfNullable(destination),
          Util.copyOfNullable(source),
          Optional.ofNullable(response),
          Util.copyOfNullable(focus));
    }
  }

  public record Destination(Url endpoint) {

    public Destination {
      Objects.requireNonNull(endpoint);
    }

    public static Destination endpoint(Url endpoint) {
      return new Destination(endpoint);
    }
  }

  public record Source(Url endpoint) {

    public Source {
      Objects.requireNonNull(endpoint);
    }

    public static Source endpoint(Url endpoint) {
      return new Source(endpoint);
    }
  }

  public record Response(String identifier, Code code) {

    public Response {
      Objects.requireNonNull(identifier);
      Objects.requireNonNull(code);
    }

    public static Response of(String identifier, Code code) {
      return new Response(identifier, code);
    }
  }
}
