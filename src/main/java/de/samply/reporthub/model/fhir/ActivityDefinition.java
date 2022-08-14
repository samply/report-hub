package de.samply.reporthub.model.fhir;

import static de.samply.reporthub.model.fhir.PublicationStatus.ACTIVE;
import static de.samply.reporthub.model.fhir.PublicationStatus.DRAFT;
import static de.samply.reporthub.model.fhir.PublicationStatus.UNKNOWN;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition.Builder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record ActivityDefinition(
    Optional<String> id,
    Optional<Meta> meta,
    Optional<String> url,
    Optional<String> title,
    Code status,
    List<RelatedArtifact> relatedArtifact,
    Optional<CodeableConcept> code) implements Resource<ActivityDefinition> {

  public ActivityDefinition {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(url);
    Objects.requireNonNull(title);
    Objects.requireNonNull(status);
    Objects.requireNonNull(relatedArtifact);
    Objects.requireNonNull(code);
  }

  @Override
  public ActivityDefinition withId(String id) {
    return new Builder(this).withId(id).build();
  }

  public static Builder draft() {
    return new Builder(DRAFT.code());
  }

  public static Builder active() {
    return new Builder(ACTIVE.code());
  }

  public static Builder unknown() {
    return new Builder(UNKNOWN.code());
  }

  public static Builder builder(Code status) {
    return new Builder(status);
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private String url;
    private String title;
    private Code status;
    private List<RelatedArtifact> relatedArtifact;
    private CodeableConcept code;

    public Builder() {
    }

    private Builder(Code status) {
      this.status = Objects.requireNonNull(status);
    }

    private Builder(ActivityDefinition definition) {
      id = definition.id.orElse(null);
      meta = definition.meta.orElse(null);
      url = definition.url.orElse(null);
      title = definition.title.orElse(null);
      status = definition.status;
      relatedArtifact = definition.relatedArtifact;
      code = definition.code.orElse(null);
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withUrl(String url) {
      this.url = Objects.requireNonNull(url);
      return this;
    }

    public Builder withTitle(String title) {
      this.title = Objects.requireNonNull(title);
      return this;
    }

    public Builder withStatus(Code status) {
      this.status = Objects.requireNonNull(status);
      return this;
    }

    public Builder withRelatedArtifact(List<RelatedArtifact> relatedArtifact) {
      this.relatedArtifact = relatedArtifact;
      return this;
    }

    public Builder withCode(CodeableConcept code) {
      this.code = Objects.requireNonNull(code);
      return this;
    }

    public ActivityDefinition build() {
      return new ActivityDefinition(
          Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Optional.ofNullable(url),
          Optional.ofNullable(title),
          status,
          Util.copyOfNullable(relatedArtifact),
          Optional.ofNullable(code));
    }
  }
}
