package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.ActivityDefinition.Builder;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record ActivityDefinition(
    Optional<String> id,
    Optional<String> url,
    Optional<String> title,
    Code status) implements Resource {

  public ActivityDefinition {
    Objects.requireNonNull(id);
    Objects.requireNonNull(url);
    Objects.requireNonNull(title);
    Objects.requireNonNull(status);
  }

  public static Builder builder(Code status) {
    return new Builder(status);
  }

  public static class Builder {

    private String id;
    private String url;
    private String title;
    private Code status;

    public Builder() {
    }

    private Builder(Code status) {
      this.status = Objects.requireNonNull(status);
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
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

    public ActivityDefinition build() {
      return new ActivityDefinition(
          Optional.ofNullable(id),
          Optional.ofNullable(url),
          Optional.ofNullable(title),
          status);
    }
  }
}
