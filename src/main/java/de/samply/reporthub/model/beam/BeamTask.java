package de.samply.reporthub.model.beam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.samply.reporthub.model.beam.BeamTask.FailureStrategy.Retry;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(Include.NON_EMPTY)
public record BeamTask(UUID id, String from, List<String> to, String metadata, String body,
                       int ttl, FailureStrategy failure_strategy) {

  public static final FailureStrategy DEFAULT_FAILURE_STRATEGY =
      new FailureStrategy(new Retry(1000, 5));

  public BeamTask {
    Objects.requireNonNull(id);
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    Objects.requireNonNull(metadata);
    Objects.requireNonNull(body);
    Objects.requireNonNull(failure_strategy);
  }

  public static BeamTask of(UUID id, String from, List<String> to, int ttl, String body) {
    return new BeamTask(id, from, to, "foo", body, ttl, DEFAULT_FAILURE_STRATEGY);
  }

  public record FailureStrategy(Retry retry) {

    public record Retry(int backoff_millisecs, int max_tries) {

    }
  }
}
