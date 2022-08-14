package de.samply.reporthub.dktk.web.model;

import de.samply.reporthub.web.model.Link;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record EvaluateMeasureTask(
    String id,
    String status,
    Optional<Link> measureLink,
    Optional<Link> reportLink,
    Optional<String> error,
    List<HistoryListItem> history) {

  public EvaluateMeasureTask {
    Objects.requireNonNull(id);
    Objects.requireNonNull(status);
    Objects.requireNonNull(measureLink);
    Objects.requireNonNull(reportLink);
    Objects.requireNonNull(error);
    Objects.requireNonNull(history);
  }

  public record HistoryListItem(
      OffsetDateTime lastModified,
      String status,
      Optional<Link> reportLink) {

    public HistoryListItem {
      Objects.requireNonNull(status);
      Objects.requireNonNull(lastModified);
      Objects.requireNonNull(reportLink);
    }
  }
}
