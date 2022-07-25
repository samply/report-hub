package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Library.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Library(
    Optional<String> id,
    Optional<String> url,
    Optional<String> name,
    Code status,
    CodeableConcept type,
    Optional<CodeableConcept> subjectCodeableConcept,
    List<Attachment> content) implements Resource {

  public Library {
    Objects.requireNonNull(status);
    Objects.requireNonNull(type);
  }

  public Library addContent(Attachment content) {
    List<Attachment> list = new ArrayList<>(this.content);
    list.add(content);
    return new Builder(this).withContent(list).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String id;
    private String url;
    private String name;
    private Code status;
    private CodeableConcept type;
    private CodeableConcept subjectCodeableConcept;
    private List<Attachment> content;

    public Builder() {
    }

    private Builder(Library library) {
      this.id = library.id.orElse(null);
      this.url = library.url.orElse(null);
      this.name = library.name.orElse(null);
      this.status = library.status;
      this.type = library.type;
      this.subjectCodeableConcept = library.subjectCodeableConcept.orElse(null);
      this.content = library.content;
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withUrl(String url) {
      this.url = Objects.requireNonNull(url);
      return this;
    }

    public Builder withName(String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    public Builder withStatus(Code status) {
      this.status = Objects.requireNonNull(status);
      return this;
    }

    public Builder withType(CodeableConcept type) {
      this.type = Objects.requireNonNull(type);
      return this;
    }

    public Builder withSubjectCodeableConcept(CodeableConcept subjectCodeableConcept) {
      this.subjectCodeableConcept = Objects.requireNonNull(subjectCodeableConcept);
      return this;
    }

    public Builder withContent(List<Attachment> content) {
      this.content = content;
      return this;
    }

    public Library build() {
      return new Library(Optional.ofNullable(id),
          Optional.ofNullable(url),
          Optional.ofNullable(name),
          status,
          type,
          Optional.ofNullable(subjectCodeableConcept),
          Util.copyOfNullable(content));
    }
  }
}
