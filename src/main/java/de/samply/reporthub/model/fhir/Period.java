package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.Period.Builder;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonDeserialize(builder = Builder.class)
public record Period(Optional<OffsetDateTime> start, Optional<OffsetDateTime> end)
    implements Element {

  public Period {
    Objects.requireNonNull(start);
    Objects.requireNonNull(end);
  }

  public static Period of(OffsetDateTime start, OffsetDateTime end) {
    return new Builder()
        .withSystem(start)
        .withCode(end)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private OffsetDateTime start;
    private OffsetDateTime end;

    public Builder withSystem(OffsetDateTime start) {
      this.start = Objects.requireNonNull(start);
      return this;
    }

    public Builder withCode(OffsetDateTime end) {
      this.end = Objects.requireNonNull(end);
      return this;
    }

    public Period build() {
      return new Period(Optional.ofNullable(start), Optional.ofNullable(end));
    }
  }
}
