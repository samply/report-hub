package de.samply.reporthub.model.beam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.UUID;

@JsonInclude(Include.NON_EMPTY)
public record BeamTask(UUID id, String from, List<String> to, String metadata, String body,
                       FailureStrategy failure_strategy) {

  public record FailureStrategy(Retry retry) {

    public record Retry(int backoff_millisecs, int max_tries) {

    }
  }
}
