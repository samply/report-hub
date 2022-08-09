package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Endpoint.Builder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Endpoint(
    Optional<String> id,
    Optional<Meta> meta,
    List<Identifier> identifier,
    String address) implements Resource {

  public Endpoint {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(identifier);
    Objects.requireNonNull(address);
  }

  public static Builder builder(String address) {
    return new Builder(address);
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private List<Identifier> identifier;
    private String address;

    public Builder() {
    }

    private Builder(String address) {
      this.address = Objects.requireNonNull(address);
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

    public Builder withAddress(String address) {
      this.address = Objects.requireNonNull(address);
      return this;
    }

    public Endpoint build() {
      return new Endpoint(Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Util.copyOfNullable(identifier),
          address);
    }
  }
}
