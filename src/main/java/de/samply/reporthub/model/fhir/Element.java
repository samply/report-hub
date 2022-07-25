package de.samply.reporthub.model.fhir;

import java.util.Optional;

public interface Element {

  //Optional<String> id();

  //List<Extension> extension();

  default <T extends Element> Optional<T> cast(Class<T> type) {
    return type.isInstance(this) ? Optional.of(type.cast(this)) : Optional.empty();
  }
}
