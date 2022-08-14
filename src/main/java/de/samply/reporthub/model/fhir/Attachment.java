package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.Attachment.Builder;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonDeserialize(builder = Builder.class)
public record Attachment(Optional<Code> contentType, Optional<Base64Binary> data) implements
    Element {

  public Attachment {
    Objects.requireNonNull(contentType);
    Objects.requireNonNull(data);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Code contentType;
    private Base64Binary data;

    public Builder withContentType(Code contentType) {
      this.contentType = Objects.requireNonNull(contentType);
      return this;
    }

    public Builder withData(Base64Binary data) {
      this.data = Objects.requireNonNull(data);
      return this;
    }

    public Attachment build() {
      return new Attachment(Optional.ofNullable(contentType), Optional.ofNullable(data));
    }
  }
}
