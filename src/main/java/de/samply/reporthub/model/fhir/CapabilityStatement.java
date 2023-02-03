package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.CapabilityStatement.Builder;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record CapabilityStatement(
    Optional<String> id,
    Optional<Meta> meta,
    Optional<Software> software) implements Resource<CapabilityStatement> {

  public CapabilityStatement {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(software);
  }

  @Override
  public CapabilityStatement withId(String id) {
    return new Builder(this).withId(id).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private Software software;

    public Builder() {
    }

    private Builder(CapabilityStatement statement) {
      id = statement.id.orElse(null);
      meta = statement.meta.orElse(null);
      software = statement.software.orElse(null);
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withSoftware(Software software) {
      this.software = Objects.requireNonNull(software);
      return this;
    }

    public CapabilityStatement build() {
      return new CapabilityStatement(
          Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Optional.ofNullable(software));
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonDeserialize(builder = Software.Builder.class)
  public record Software(
      String name,
      Optional<String> version,
      @JsonFormat(pattern = "yyyy-MM-dd")
      Optional<LocalDate> releaseDate) implements BackboneElement {

    public Software {
      Objects.requireNonNull(name);
      Objects.requireNonNull(version);
      Objects.requireNonNull(releaseDate);
    }

    public static Builder builder(String name) {
      return new Builder(name);
    }

    public static class Builder {

      private String name;
      private String version;
      @JsonFormat(pattern = "yyyy-MM-dd")
      private LocalDate releaseDate;

      public Builder() {
      }

      private Builder(String name) {
        this.name = Objects.requireNonNull(name);
      }

      public Builder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
      }

      public Builder withVersion(String version) {
        this.version = Objects.requireNonNull(version);
        return this;
      }

      public Builder withReleaseDate(LocalDate releaseDate) {
        this.releaseDate = Objects.requireNonNull(releaseDate);
        return this;
      }

      public Software build() {
        return new Software(name, Optional.ofNullable(version), Optional.ofNullable(releaseDate));
      }
    }
  }
}
