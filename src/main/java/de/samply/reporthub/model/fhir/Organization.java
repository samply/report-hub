package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Organization.Builder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Organization(
    Optional<String> id,
    Optional<Meta> meta,
    List<Identifier> identifier,
    List<Reference> endpoint) implements Resource<Organization> {

  public Organization {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(identifier);
    Objects.requireNonNull(endpoint);
  }

  @Override
  public Organization withId(String id) {
    return new Builder(this).withId(id).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private List<Identifier> identifier;
    private List<Reference> endpoint;

    public Builder() {
    }

    private Builder(Organization organization) {
      id = organization.id.orElse(null);
      meta = organization.meta.orElse(null);
      identifier = organization.identifier;
      endpoint = organization.endpoint;
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withIdentifier(List<Identifier> identifier) {
      this.identifier = identifier;
      return this;
    }

    public Builder withEndpoint(List<Reference> endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Organization build() {
      return new Organization(Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Util.copyOfNullable(identifier),
          Util.copyOfNullable(endpoint));
    }
  }
}
