package de.samply.reporthub.component;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class BeamClientTest {

  private BeamClient client;

  @BeforeEach
  void setUp() {
    client = new BeamClient(WebClient.builder()
        .baseUrl("http://localhost:8081")
        .defaultHeader("Accept", APPLICATION_JSON_VALUE)
        .defaultHeader("Authorization", "ApiKey app1.proxy1.broker App1Secret")
        .build(), "app1.proxy1.broker");
  }

  @Test
  @Disabled("Only works with running Beam")
  void retrieveTasks_empty() {
    var result = client.retrieveTasks();

    StepVerifier.create(result)
        .expectNextMatches(task -> {
          System.out.println("task = " + task);
          return true;
        })
        .verifyComplete();
  }
}
