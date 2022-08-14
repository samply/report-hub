package de.samply.reporthub.service.fhir.store;

import java.util.Objects;

/**
 * Exception for {@code NotFound} were a single FHIR resource was requested.
 */
public final class ResourceNotFoundException extends Exception {

  private final String type;
  private final String id;

  public ResourceNotFoundException(String type, String id) {
    super("%s with id `%s` was not found.".formatted(type, id));
    this.type = Objects.requireNonNull(type);
    this.id = Objects.requireNonNull(id);
  }

  public String type() {
    return type;
  }

  public String id() {
    return id;
  }
}
