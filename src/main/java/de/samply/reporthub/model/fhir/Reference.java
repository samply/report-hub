package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.Reference.Builder;
import de.samply.reporthub.service.fhir.store.Store;
import java.util.Objects;
import java.util.Optional;
import reactor.core.publisher.Mono;

@JsonInclude(Include.NON_EMPTY)
@JsonDeserialize(builder = Builder.class)
public record Reference(Optional<String> reference) implements Element {

  public Reference {
    Objects.requireNonNull(reference);
  }

  public static Reference ofReference(String reference) {
    return new Builder().withReference(reference).build();
  }

  public static Reference ofReference(String type, String id) {
    return ofReference(type + "/" + id);
  }

  public <T extends Resource<T>> Mono<T> resolve(Store store, Class<T> type) {
    return Mono.justOrEmpty(logicalId(type)).flatMap(id -> store.fetchResource(type, id));
  }

  public Optional<String> logicalId(Class<? extends Resource<?>> type) {
    return reference.flatMap(ref -> {
      var parts = ref.split("/", 2);
      return (parts.length == 2 && type.getSimpleName().equals(parts[0]))
          ? Optional.of(parts[1]) : Optional.empty();
    });
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String reference;

    public Builder withReference(String reference) {
      this.reference = Objects.requireNonNull(reference);
      return this;
    }

    public Reference build() {
      return new Reference(Optional.ofNullable(reference));
    }
  }
}
