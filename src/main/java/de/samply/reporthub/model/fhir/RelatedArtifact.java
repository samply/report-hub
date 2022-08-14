package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.RelatedArtifact.Builder;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonDeserialize(builder = Builder.class)
public record RelatedArtifact(Code type, Optional<Canonical> resource) implements Element {

  public RelatedArtifact {
    Objects.requireNonNull(type);
    Objects.requireNonNull(resource);
  }

  public static Builder dependsOn() {
    return new Builder(RelatedArtifactType.DEPENDS_ON.code());
  }

  public static class Builder {

    private Code type;
    private Canonical resource;

    public Builder() {
    }

    private Builder(Code type) {
      this.type = Objects.requireNonNull(type);
    }

    public Builder withType(Code type) {
      this.type = Objects.requireNonNull(type);
      return this;
    }

    public Builder withResource(Canonical resource) {
      this.resource = Objects.requireNonNull(resource);
      return this;
    }

    public RelatedArtifact build() {
      return new RelatedArtifact(type, Optional.ofNullable(resource));
    }
  }
}
