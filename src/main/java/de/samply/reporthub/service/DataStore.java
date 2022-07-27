package de.samply.reporthub.service;

import static de.samply.reporthub.model.fhir.BundleType.TRANSACTION;
import static de.samply.reporthub.model.fhir.HttpVerb.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Bundle;
import de.samply.reporthub.model.fhir.Bundle.Entry;
import de.samply.reporthub.model.fhir.Bundle.Entry.Request;
import de.samply.reporthub.model.fhir.CapabilityStatement;
import de.samply.reporthub.model.fhir.Library;
import de.samply.reporthub.model.fhir.Measure;
import de.samply.reporthub.model.fhir.MeasureReport;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class DataStore implements Store {

  private static final Logger logger = LoggerFactory.getLogger(DataStore.class);

  private final WebClient client;

  public DataStore(@Qualifier("dataStoreClient") WebClient client) {
    this.client = client;
  }

  public Mono<CapabilityStatement> fetchMetadata() {
    logger.debug("Fetching metadata...");
    return client.get()
        .uri("/metadata")
        .retrieve()
        .bodyToMono(CapabilityStatement.class)
        .doOnError(e -> logger.warn("Error while fetching metadata: {}", e.getMessage()));
  }

  public Mono<Measure> createMeasure(Measure measure) {
    return measure.url().map(url -> client.post()
        .uri("/Measure")
        .contentType(APPLICATION_JSON)
        .header("If-None-Exist", "url=%s".formatted(url))
        .bodyValue(measure)
        .retrieve()
        .bodyToMono(Measure.class)
    ).orElse(Mono.error(new Exception("Missing Measure URL.")));
  }

  public Mono<MeasureReport> evaluateMeasure(String url) {
    return client.get()
        .uri("/Measure/$evaluate-measure?measure={url}&periodStart=1900&periodEnd=2200", url)
        .retrieve()
        .bodyToMono(MeasureReport.class);
  }

  public Mono<Bundle> createMeasureAndLibrary(Measure measure, Library library) {
    return measureLibraryBundle(measure, library).flatMap(this::transact);
  }

  private Mono<Bundle> transact(Bundle bundle) {
    return client.post()
        .contentType(APPLICATION_JSON)
        .header("Prefer", "return=representation")
        .bodyValue(bundle)
        .retrieve()
        .bodyToMono(Bundle.class)
        .retryWhen(Retry.backoff(5, Duration.ofSeconds(1)));
  }

  private Mono<Bundle> measureLibraryBundle(Measure measure, Library library) {
    return measure.url()
        .map(measureUrl -> library.url()
            .map(libraryUrl -> Mono.just(Bundle.builder(TRANSACTION.code())
                .withEntry(List.of(Entry.builder()
                    .withResource(measure)
                    .withRequest(Request.builder()
                        .withMethod(POST.code())
                        .withUrl("Measure")
                        .withIfNoneExist("url=%s".formatted(measureUrl))
                        .build())
                    .build(), Entry.builder()
                    .withResource(library)
                    .withRequest(Request.builder()
                        .withMethod(POST.code())
                        .withUrl("Library")
                        .withIfNoneExist("url=%s".formatted(libraryUrl))
                        .build())
                    .build()))
                .build()))
            .orElse(Mono.error(new Exception("Missing Library URL."))))
        .orElse(Mono.error(new Exception("Missing Measure URL.")));
  }
}
