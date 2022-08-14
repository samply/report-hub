package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.Instant;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
public record Meta(Optional<String> versionId, Optional<Instant> lastUpdated) implements Element {

}
