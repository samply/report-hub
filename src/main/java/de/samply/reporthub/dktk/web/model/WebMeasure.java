package de.samply.reporthub.dktk.web.model;

import java.util.Objects;

public record WebMeasure(String url, String title, String status, String library) {

  public WebMeasure {
    Objects.requireNonNull(url);
    Objects.requireNonNull(title);
    Objects.requireNonNull(status);
    Objects.requireNonNull(library);
  }
}
