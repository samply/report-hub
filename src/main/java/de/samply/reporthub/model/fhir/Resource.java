package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Optional;

@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonSubTypes({
    @Type(ActivityDefinition.class),
    @Type(Bundle.class),
    @Type(CapabilityStatement.class),
    @Type(Endpoint.class),
    @Type(Library.class),
    @Type(Measure.class),
    @Type(MeasureReport.class),
    @Type(MessageHeader.class),
    @Type(OperationOutcome.class),
    @Type(Organization.class),
    @Type(Parameters.class),
    @Type(Task.class)})
public interface Resource {

  Optional<String> id();

  Optional<Meta> meta();

  default <T extends Resource> Optional<T> cast(Class<T> type) {
    return type.isInstance(this) ? Optional.of(type.cast(this)) : Optional.empty();
  }
}
