package de.samply.reporthub;

import static com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static de.samply.reporthub.service.TaskStore.BEAM_TASK_ID_SYSTEM;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.reporthub.model.fhir.Identifier;
import de.samply.reporthub.model.fhir.OperationOutcome;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

public interface Util {

  static Identifier beamTaskIdentifier(String id) {
    return Identifier.builder().withSystem(BEAM_TASK_ID_SYSTEM).withValue(id).build();
  }

  static <T> Mono<T> parseJson(String s, Class<T> type) {
    try {
      return Mono.just(mapper().readValue(s, type));
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  static <T> Mono<T> parseJson(String s, TypeReference<T> type) {
    try {
      return Mono.just(mapper().readValue(s, type));
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  static Mono<String> prettyPrintJson(Object o) {
    return prettyPrintJson(mapper(), o);
  }
  static Mono<String> prettyPrintJson(ObjectMapper mapper, Object o) {
    try {
      return Mono.just(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o));
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  static ObjectMapper mapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    mapper.disable(WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }

  static <E> List<E> copyOfNullable(Collection<? extends E> coll) {
    return coll == null ? List.of() : List.copyOf(coll);
  }

  static Mono<OperationOutcome> operationOutcome(WebClientResponseException e) {
    return parseJson(e.getResponseBodyAsString(), OperationOutcome.class);
  }

  static Optional<String> referenceId(String reference) {
    String[] strings = reference.split("/", 2);
    return strings.length == 2 ? Optional.of(strings[1]) : Optional.empty();
  }
}
