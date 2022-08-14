package de.samply.reporthub.model.beam;

import static de.samply.reporthub.model.beam.BeamResult.Status.CLAIMED;
import static de.samply.reporthub.model.beam.BeamResult.Status.SUCCEEDED;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
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

  public static BeamResult claimed(String from, List<String> to, UUID taskId) {
    return new BeamResult(from, to, taskId, CLAIMED, "foo", Optional.empty());
  }

  public static BeamResult base64Succeeded(String from, List<String> to, UUID taskId, String body) {
    return base64Body(from, to, taskId, SUCCEEDED, body);
  }

  public static BeamResult base64Body(String from, List<String> to, UUID taskId, Status status,
      String body) {
    return new BeamResult(from, to, taskId, status, "foo", Optional.of(base64Encode(body)));
  }

  public enum Status {
    @JsonProperty("claimed") CLAIMED,
    @JsonProperty("succeeded") SUCCEEDED,
    @JsonProperty("tempfailed") TEMP_FAILED,
    @JsonProperty("permfailed") PERM_FAILED
  }

  private static String base64Encode(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes(UTF_8));
  }
}
