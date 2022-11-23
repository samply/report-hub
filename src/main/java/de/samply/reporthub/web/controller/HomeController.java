package de.samply.reporthub.web.controller;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.seeOther;

import de.samply.reporthub.dktk.model.fhir.TaskCode;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.service.TaskCreator;
import de.samply.reporthub.service.fhir.store.TaskStore;
import de.samply.reporthub.util.Monos;
import de.samply.reporthub.util.Optionals;
import de.samply.reporthub.web.model.CreateTaskFormActivityDefinition;
import de.samply.reporthub.web.model.Link;
import de.samply.reporthub.web.model.TaskLineItem;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
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
  private final GenericApplicationContext applicationContext;

  public HomeController(TaskStore taskStore, GenericApplicationContext applicationContext) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.applicationContext = Objects.requireNonNull(applicationContext);
  }

  @Bean
  public RouterFunction<ServerResponse> homeRouter() {
    return route(GET(""), this::handle)
        .andRoute(POST("create-task").and(accept(APPLICATION_FORM_URLENCODED)),
            this::createTask);
  }

  public Mono<ServerResponse> handle(ServerRequest request) {
    logger.debug("Request home page");
    return homeModel().flatMap(model -> ok().render("home", model));
  }

  Mono<Map<String, Object>> homeModel() {
    return Monos.map(taskLineItems().collectList(),
            taskStore.listAllActivityDefinitions().collectList(), HomeController::homeModel)
        .onErrorResume(e -> Mono.just(Map.of("error", "Error while loading tasks.")));
  }

  static Map<String, Object> homeModel(List<TaskLineItem> taskLineItems,
      List<ActivityDefinition> activityDefinitions) {
    return Map.of("taskLineItems", taskLineItems,
        "createTaskFormActivityDefinitions",
        createTaskFormActivityDefinitions(activityDefinitions));
  }

  Mono<ServerResponse> createTask(ServerRequest request) {
    logger.debug("Create task");
    return request.formData()
        .flatMap(this::formTask)
        .flatMap(taskStore::createTask)
        .flatMap(task -> seeOther(request.uriBuilder().path("/..").build()).build());
  }

  Mono<Task> formTask(MultiValueMap<String, String> formData) {
    return taskStore.findByUrl(ActivityDefinition.class, formData.getFirst("instantiates"))
        .flatMap(this::formTask);
  }

  private Mono<Task> formTask(ActivityDefinition activityDefinition) {
    return Mono.justOrEmpty(activityDefinition.url())
        .map(url -> applicationContext.getBean(url, TaskCreator.class))
        .flatMap(creator -> creator.create(activityDefinition));
  }

  private Flux<TaskLineItem> taskLineItems() {
    return taskStore.listNewestTasks()
        .flatMap(task -> Mono.justOrEmpty(taskLineItem(task)));
  }

  private Optional<TaskLineItem> taskLineItem(Task task) {
    return Optionals.map(task.lastModified(),
        task.code().flatMap(code -> code.findCodeValue(TaskCode.CODE_SYSTEM_URL)),
        task.id(), task.status().value(), (lastModified, code, id, status) -> TaskLineItem.of(
            lastModified,
            Link.of(URI.create("/task/%s/%s".formatted(code, id)), id),
            code,
            status
        ));
  }

  static List<CreateTaskFormActivityDefinition> createTaskFormActivityDefinitions(
      List<ActivityDefinition> activityDefinitions) {
    return activityDefinitions.stream()
        .flatMap(activityDefinition -> Optionals.map(activityDefinition.url(),
            activityDefinition.title(), CreateTaskFormActivityDefinition::new).stream())
        .toList();
  }
}
