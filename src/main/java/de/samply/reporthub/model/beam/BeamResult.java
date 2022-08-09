package de.samply.reporthub.model.beam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@JsonInclude(Include.NON_EMPTY)
public record BeamResult(
    String from,
    List<String> to,
    UUID task,
    Status status,
    String metadata,
    Optional<String> body) {

  public BeamResult {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    Objects.requireNonNull(task);
    Objects.requireNonNull(status);
    Objects.requireNonNull(metadata);
    Objects.requireNonNull(body);
  }

  public enum Status {
    @JsonProperty("claimed") CLAIMED,
    @JsonProperty("succeeded") SUCCEEDED,
    @JsonProperty("tempfailed") TEMP_FAILED,
    @JsonProperty("permfailed") PERM_FAILED
  }
}
