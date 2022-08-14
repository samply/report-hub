package de.samply.reporthub.web.model;

import java.net.URI;
import java.util.Objects;

public record Link(URI href, String label) {

  public Link {
    Objects.requireNonNull(href);
    Objects.requireNonNull(label);
  }

  public static Link of(URI href, String label) {
    return new Link(href, label);
  }
}
