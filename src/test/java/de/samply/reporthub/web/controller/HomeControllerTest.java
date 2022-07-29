package de.samply.reporthub.web.controller;

import static de.samply.reporthub.web.controller.ServerResponseAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.SEE_OTHER;

import de.samply.reporthub.service.TaskStore;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

  private static final String ACTIVITY_DEFINITION_URL = "url-155958";
  private static final String TASK_ID = "id-160606";

  @Mock
  private TaskStore taskStore;

  private HomeController controller;

  @BeforeEach
  void setUp() {
    controller = new HomeController(taskStore, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
  }

  @Test
  void createTask() {
    var request = mock(ServerRequest.class);
    var formData = new LinkedMultiValueMap<>(Map.of("instantiates",
        List.of(ACTIVITY_DEFINITION_URL)));
    when(request.formData()).thenReturn(Mono.just(formData));
    when(request.uriBuilder()).thenAnswer(
        invocation -> UriComponentsBuilder.fromUriString("/create-task"));
    var taskToCreate = controller.requestedTask(formData);
    when(taskStore.createTask(taskToCreate)).thenReturn(Mono.just(taskToCreate.withId(TASK_ID)));

    var response = controller.createTask(request).block();

    assertThat(response)
        .hasStatusCode(SEE_OTHER)
        .hasLocation(URI.create("/"));
  }
}
