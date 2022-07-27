package de.samply.reporthub.web.controller;

import static org.springframework.http.HttpStatus.SEE_OTHER;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.TaskStatus;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.web.model.CreateTaskFormActivityDefinition;
import de.samply.reporthub.web.model.WebTask;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class HomeController {

  private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

  private final TaskStore taskStore;
  private final Clock clock;

  public HomeController(TaskStore taskStore, Clock clock) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.clock = Objects.requireNonNull(clock);
  }

  @GetMapping("/")
  public String home(UriComponentsBuilder uriBuilder, Model model) {
    var allActivityDefinitions = taskStore.listAllActivityDefinitions();
    model.addAttribute("createTaskFormActivityDefinitions", createTaskFormActivityDefinitions(
        allActivityDefinitions));
    model.addAttribute("tasks", tasks(uriBuilder, allActivityDefinitions));
    return "home";
  }

  @PostMapping(value = "/create-task", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public Mono<ResponseEntity<Void>> createTask(UriComponentsBuilder uriBuilder,
      ServerWebExchange serverWebExchange) {
    return serverWebExchange.getFormData()
        .flatMap(formData -> taskStore.createTask(requestedTask(formData)))
        .map(task -> ResponseEntity.status(SEE_OTHER).location(uriBuilder.build().toUri()).build());
  }

  private Task requestedTask(MultiValueMap<String, String> formData) {
    return Task.builder(TaskStatus.REQUESTED.code())
        .withInstantiatesCanonical(formData.getFirst("instantiates"))
        .withLastModified(OffsetDateTime.now(clock))
        .build();
  }

  private Flux<CreateTaskFormActivityDefinition> createTaskFormActivityDefinitions(
      Flux<ActivityDefinition> allActivityDefinitions) {
    return allActivityDefinitions.map(activityDefinition -> activityDefinition.url()
            .flatMap(url -> activityDefinition.title()
                .map(title -> new CreateTaskFormActivityDefinition(url, title))))
        .flatMap(Mono::justOrEmpty);
  }

  private Flux<WebTask> tasks(UriComponentsBuilder uriBuilder,
      Flux<ActivityDefinition> allActivityDefinitions) {
    return allActivityDefinitions
        .filter(activityDefinition -> activityDefinition.url().isPresent())
        .collectMap(activityDefinition -> activityDefinition.url().orElseThrow())
        .map(activityDefinitions -> new WebTasksBuilder(uriBuilder, activityDefinitions))
        .flatMapMany(webTasksBuilder -> taskStore.listAllTasks()
            .map(webTasksBuilder::webTask)
            .flatMap(Mono::justOrEmpty))
        // TODO: sort with FHIR when Blaze is able to
        .sort(Comparator.comparing(WebTask::lastModified, Comparator.reverseOrder()));
  }

  private static class WebTasksBuilder extends AbstractWebTasksBuilder {

    private final Map<String, ActivityDefinition> activityDefinitions;

    private WebTasksBuilder(UriComponentsBuilder uriBuilder,
        Map<String, ActivityDefinition> activityDefinitions) {
      super(uriBuilder);
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
