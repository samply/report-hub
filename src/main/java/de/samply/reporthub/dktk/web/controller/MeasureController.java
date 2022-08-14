package de.samply.reporthub.dktk.web.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import de.samply.reporthub.dktk.web.model.WebMeasure;
import de.samply.reporthub.model.fhir.Attachment;
import de.samply.reporthub.model.fhir.Base64Binary;
import de.samply.reporthub.model.fhir.Canonical;
import de.samply.reporthub.model.fhir.Library;
import de.samply.reporthub.model.fhir.Measure;
import de.samply.reporthub.service.fhir.store.DataStore;
import de.samply.reporthub.service.fhir.store.ResourceNotFoundException;
import de.samply.reporthub.util.Monos;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class MeasureController {

  private static final Logger logger = LoggerFactory.getLogger(MeasureController.class);

  private final DataStore dataStore;

  public MeasureController(DataStore dataStore) {
    this.dataStore = Objects.requireNonNull(dataStore);
  }

  /**
   * Produces the router function for the {@code dktk/measure/{id}} endpoint.
   *
   * @return the router function for the {@code dktk/measure/{id}} endpoint
   */
  @Bean
  public RouterFunction<ServerResponse> dktkMeasureRouter() {
    return route(GET("dktk/measure/{id}"), this::handle);
  }

  Mono<ServerResponse> handle(ServerRequest request) {
    String id = request.pathVariable("id");
    logger.debug("Request Measure with id: {}", id);
    return dataStore.fetchResource(Measure.class, id)
        .flatMap(this::webMeasure)
        .flatMap(measure -> ok().render("dktk/measure", Map.of("measure", measure)))
        .onErrorResume(ResourceNotFoundException.class, MeasureController::notFound);
  }

  private Mono<WebMeasure> webMeasure(Measure measure) {
    return Monos.map(url(measure), status(measure), library(measure),
        (url, status, library) -> new WebMeasure(url, measure.title().or(measure::name)
            .orElse("unknown"), status, library));
  }

  private static Mono<String> url(Measure measure) {
    return Monos.justOrError(measure.url(), () -> new Exception("Missing Measure URL."));
  }

  private static Mono<String> status(Measure measure) {
    return Monos.justOrError(measure.status().value(),
        () -> new Exception("Missing Measure status."));
  }

  private Mono<String> library(Measure measure) {
    return libraryUrl(measure)
        .flatMap(url -> dataStore.findByUrl(Library.class, url))
        .flatMap(MeasureController::data);
  }

  private static Mono<String> libraryUrl(Measure measure) {
    return Monos.justOrError(measure.library().stream().findFirst().flatMap(Canonical::value),
        () -> new Exception("Missing Library URL."));
  }

  private static Mono<String> data(Library library) {
    return Monos.justOrError(library.content().stream().findFirst().flatMap(Attachment::data)
            .flatMap(Base64Binary::decodedValue).map(bytes -> new String(bytes, UTF_8)),
        () -> new Exception("Missing Library data."));
  }

  private static Mono<ServerResponse> notFound(ResourceNotFoundException e) {
    var error = "The Measure with id `%s` was not found.".formatted(e.id());
    logger.warn(error);
    return ok().render("404", Map.of("error", error));
  }
}
