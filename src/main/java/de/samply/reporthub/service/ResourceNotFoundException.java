package de.samply.reporthub.service;

public class ResourceNotFoundException extends Exception {

  private final String type;
  private final String id;

  public ResourceNotFoundException(String type, String id) {
    super("%s with id `%s` was not found.".formatted(type, id));
    this.type = type;
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public String getId() {
    return id;
  }
}
