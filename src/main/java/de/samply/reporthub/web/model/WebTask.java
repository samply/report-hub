package de.samply.reporthub.web.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record WebTask(
    String id,
    Link activityDefinitionLink,
    String status,
    OffsetDateTime lastModified,
    Optional<Link> reportLink,
    List<WebTask> history) {

  public WebTask {
    Objects.requireNonNull(id);
    Objects.requireNonNull(activityDefinitionLink);
    Objects.requireNonNull(status);
    Objects.requireNonNull(lastModified);
    Objects.requireNonNull(reportLink);
    Objects.requireNonNull(history);
  }
}
