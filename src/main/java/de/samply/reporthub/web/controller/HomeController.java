package de.samply.reporthub.web.controller;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.seeOther;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.TaskStatus;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.util.Monos;
import de.samply.reporthub.util.Optionals;
import de.samply.reporthub.web.model.CreateTaskFormActivityDefinition;
import de.samply.reporthub.web.model.WebTask;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class HomeController {

  private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

  private final TaskStore taskStore;
  private final Clock clock;

  public HomeController(TaskStore taskStore, Clock clock) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.clock = Objects.requireNonNull(clock);
  }

  @Bean
  public RouterFunction<ServerResponse> homeRouter() {
    return route(GET("/"), this::handle)
        .andRoute(POST("/create-task").and(accept(APPLICATION_FORM_URLENCODED)),
            this::createTask);
  }

  public Mono<ServerResponse> handle(ServerRequest request) {
    logger.debug("Request home page");
    return homeModel(request).flatMap(model -> ok().render("home", model));
  }

  Mono<Map<String, Object>> homeModel(ServerRequest request) {
    return Monos.map(taskStore.listAllActivityDefinitions().collectList(),
            activityDefinitions -> tasks(request, activityDefinitions).collectList(),
            HomeController::homeModel)
        .onErrorResume(e -> Mono.just(Map.of("error", "Error while loading tasks.")));
  }

  static Map<String, Object> homeModel(List<ActivityDefinition> activityDefinitions,
      List<WebTask> tasks) {
    return Map.of("createTaskFormActivityDefinitions",
        createTaskFormActivityDefinitions(activityDefinitions),
        "tasks", tasks);
  }

  Mono<ServerResponse> createTask(ServerRequest request) {
    logger.debug("Create task");
    return request.formData()
        .flatMap(formData -> taskStore.createTask(requestedTask(formData)))
        .flatMap(task -> seeOther(request.uriBuilder().replacePath("/").build()).build());
  }

  Task requestedTask(MultiValueMap<String, String> formData) {
    return Task.builder(TaskStatus.REQUESTED.code())
        .withInstantiatesCanonical(formData.getFirst("instantiates"))
        .withLastModified(OffsetDateTime.now(clock))
        .build();
  }

  static List<CreateTaskFormActivityDefinition> createTaskFormActivityDefinitions(
      List<ActivityDefinition> activityDefinitions) {
    return activityDefinitions.stream()
        .map(activityDefinition -> Optionals.map(activityDefinition.url(),
            activityDefinition.title(), CreateTaskFormActivityDefinition::new))
        .flatMap(Optional::stream)
        .toList();
  }

  private Flux<WebTask> tasks(ServerRequest request,
      List<ActivityDefinition> allActivityDefinitions) {
    var map = allActivityDefinitions.stream()
        .filter(activityDefinition -> activityDefinition.url().isPresent())
        .collect(Collectors.toMap(activityDefinition -> activityDefinition.url().orElseThrow(),
            Function.identity()));
    var builder = new WebTasksBuilder(request, map);
    return taskStore.listNewestTasks()
        .map(builder::webTask)
        .flatMap(Mono::justOrEmpty);
  }

  private static class WebTasksBuilder extends AbstractWebTasksBuilder {

    private final Map<String, ActivityDefinition> activityDefinitions;

    private WebTasksBuilder(ServerRequest request,
        Map<String, ActivityDefinition> activityDefinitions) {
      super(request);
      this.activityDefinitions = Objects.requireNonNull(activityDefinitions);
    }

    private Optional<WebTask> webTask(Task task) {
      return activityDefinition(task)
          .flatMap(activityDefinition -> task.id()
              .flatMap(id -> task.status().value()
                  .flatMap(status -> activityDefinitionLink(activityDefinition)
                      .flatMap(activityDefinitionLink -> task.lastModified()
                          .map(lastModified -> new WebTask(id, activityDefinitionLink, status,
                              lastModified, task.findOutput(TaskController.REPORT_PREDICATE)
                              .flatMap(this::reportLink), List.of()))))));
    }

    private Optional<ActivityDefinition> activityDefinition(Task task) {
      return task.instantiatesCanonical().flatMap(this::activityDefinition);
    }

    private Optional<ActivityDefinition> activityDefinition(String url) {
      return Optional.ofNullable(activityDefinitions.get(url));
    }
  }
}
