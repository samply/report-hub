package de.samply.reporthub.web.controller;

import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Reference;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.model.fhir.Task.Output;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.web.model.Link;
import de.samply.reporthub.web.model.WebTask;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class IndexController {

  private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

  public static final Predicate<CodeableConcept> REPORT_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-task-output", "measure-report");

  private final TaskStore taskStore;

  public IndexController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  @GetMapping("/")
  public String index(UriComponentsBuilder uriBuilder, Model model) {
    model.addAttribute("tasks", tasks(uriBuilder));
    return "index";
  }

  private Flux<WebTask> tasks(UriComponentsBuilder uriBuilder) {
    return taskStore.listAllActivityDefinitions()
        .filter(activityDefinition -> activityDefinition.url().isPresent())
        .collectMap(activityDefinition -> activityDefinition.url().orElseThrow())
        .map(activityDefinitions -> new WebTasksBuilder(uriBuilder, activityDefinitions))
        .flatMapMany(webTasksBuilder -> taskStore.listAllTasks()
            .map(webTasksBuilder::webTask)
            .flatMap(Mono::justOrEmpty))
        // TODO: sort with FHIR when Blaze is able to
        .sort(Comparator.comparing(WebTask::lastModified));
  }

  private record WebTasksBuilder(
      UriComponentsBuilder uriBuilder,
      Map<String, ActivityDefinition> activityDefinitions) {

    private Optional<WebTask> webTask(Task task) {
      return activityDefinition(task)
          .flatMap(activityDefinition -> task.status().value()
              .flatMap(status -> activityDefinitionLink(activityDefinition)
                  .flatMap(activityDefinitionLink -> task.lastModified()
                      .map(lastModified -> new WebTask(status, activityDefinitionLink, lastModified,
                          task.findOutput(REPORT_PREDICATE).flatMap(this::reportLink))))));
    }

    private Optional<ActivityDefinition> activityDefinition(Task task) {
      return task.instantiatesCanonical().flatMap(this::activityDefinition);
    }

    private Optional<ActivityDefinition> activityDefinition(String url) {
      return Optional.ofNullable(activityDefinitions.get(url));
    }

    private Optional<Link> activityDefinitionLink(ActivityDefinition activityDefinition) {
      return activityDefinition.title()
          .flatMap(title -> activityDefinition.id()
              .map(id -> activityDefinitionLink(id, title)));
    }

    private Link activityDefinitionLink(String id, String title) {
      return new Link(uriBuilder.cloneBuilder().path("activity-definition/{id}").build(id), title);
    }

    private Optional<Link> reportLink(Output output) {
      return output.castValue(Reference.class)
          .flatMap(Reference::reference)
          .flatMap(Util::referenceId)
          .map(this::reportLink);
    }

    private Link reportLink(String id) {
      return new Link(uriBuilder.cloneBuilder().path("exliquid-report/{id}").build(id), "Report");
    }
  }
}
