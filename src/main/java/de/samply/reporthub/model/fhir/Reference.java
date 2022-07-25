package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.Reference.Builder;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonDeserialize(builder = Builder.class)
public record Reference(Optional<String> reference) implements Element {

  public Reference {
    Objects.requireNonNull(reference);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String reference;

    public Builder withReference(String reference) {
      this.reference = Objects.requireNonNull(reference);
      return this;
    }

    public Reference build() {
      return new Reference(Optional.ofNullable(reference));
    }
  }
}
