package de.samply.reporthub.web.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public record TaskLineItem(
    OffsetDateTime lastModified,
    Link taskLink,
    String code,
    String status) {

  public TaskLineItem {
    Objects.requireNonNull(lastModified);
    Objects.requireNonNull(taskLink);
    Objects.requireNonNull(code);
    Objects.requireNonNull(status);
  }

  public static TaskLineItem of(
      OffsetDateTime lastModified,
      Link taskLink,
      String code,
      String status) {
    return new TaskLineItem(lastModified, taskLink, code, status);
  }
}
