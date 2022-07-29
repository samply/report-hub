package de.samply.reporthub.service;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError;
import reactor.test.StepVerifier;

public class DataStoreMockTest {

  private MockWebServer server;

  private DataStore dataStore;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    dataStore = new DataStore(WebClient.create("http://localhost:%d".formatted(server.getPort())));
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void fetchMetadata_500() {
    server.enqueue(new MockResponse().setResponseCode(500));

    var result = dataStore.fetchMetadata();

    StepVerifier.create(result).expectError(InternalServerError.class).verify();
  }
}
