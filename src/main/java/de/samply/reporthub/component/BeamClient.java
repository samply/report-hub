package de.samply.reporthub.component;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.samply.reporthub.model.BeamTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class BeamClient {

  private final WebClient client;

  public BeamClient(@Qualifier("beamProxy") WebClient client) {
    this.client = client;
  }

  public Flux<BeamTask> retrieveTasks() {
    return client.get()
        .uri("/v1/tasks")
        .accept(APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(BeamTask.class);
  }
}
