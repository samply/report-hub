package de.samply.reporthub.service.fhir.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.CapabilityStatement.Software;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@Testcontainers
class DataStoreContainerTest {

  private static final Logger logger = LoggerFactory.getLogger(DataStoreContainerTest.class);

  @Container
  @SuppressWarnings("resource")
  private final GenericContainer<?> blaze = new GenericContainer<>("samply/blaze:0.18")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withEnv("LOG_LEVEL", "debug")
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCode(200))
      .withLogConsumer(new Slf4jLogConsumer(logger));


  private DataStore dataStore;

  @SuppressWarnings("HttpUrlsUsage")
  @BeforeEach
  void setUp() {
    WebClient webClient = WebClient.builder()
        .baseUrl("http://%s:%d/fhir".formatted(blaze.getHost(), blaze.getFirstMappedPort()))
        .defaultRequest(request -> request.accept(APPLICATION_JSON))
        .codecs(configurer -> {
          configurer.defaultCodecs().maxInMemorySize(1024 * 1024);
          configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(Util.mapper()));
          configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(Util.mapper()));
        })
        .build();
    dataStore = new DataStore(webClient);
  }

  @Test
  void fetchMetadata() {
    var capabilityStatement = dataStore.fetchMetadata().block();

    assertThat(capabilityStatement).isNotNull();
    assertThat(capabilityStatement.software().map(Software::name)).contains("Blaze");
  }

  @Test
  void evaluateMeasure() {
    var result = dataStore.evaluateMeasure("foo");

    StepVerifier.create(result).expectError().verify();
  }
}
