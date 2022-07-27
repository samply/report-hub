package de.samply.reporthub.web.controller;

import de.samply.reporthub.model.fhir.ActivityDefinition;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Task;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.web.model.WebTask;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Controller
public class TaskController {

  private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

  public static final Predicate<CodeableConcept> REPORT_PREDICATE = CodeableConcept.containsCoding(
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-task-output", "measure-report");

  private final TaskStore taskStore;

  public TaskController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  @GetMapping("task/{id}")
  public String task(UriComponentsBuilder uriBuilder, @PathVariable("id") String id, Model model) {
    model.addAttribute("task", taskStore.fetchTask(id)
        .flatMap(new WebTasksBuilder(uriBuilder)::convert));
    return "task";
  }

  private class WebTasksBuilder extends AbstractWebTasksBuilder {

    private WebTasksBuilder(UriComponentsBuilder uriBuilder) {
      super(uriBuilder);
    }

    private Mono<WebTask> convert(Task task) {
      return findActivityDefinition(task)
          .flatMap(activityDefinition -> history(activityDefinition, task)
              .flatMap(history -> convert(activityDefinition, task, history)));
    }

    private Mono<WebTask> convert(ActivityDefinition activityDefinition, Task task,
        List<WebTask> history) {
      return Mono.justOrEmpty(activityDefinitionLink(activityDefinition)
          .flatMap(activityDefinitionLink -> task.id().flatMap(id -> task.status().value()
              .flatMap(status -> task.lastModified()
                  .map(lastModified -> new WebTask(id, activityDefinitionLink, status,
                      lastModified, task.findOutput(REPORT_PREDICATE)
                      .flatMap(this::reportLink), history))))));
    }

    private Mono<List<WebTask>> history(ActivityDefinition activityDefinition, Task task) {
      return Mono.justOrEmpty(task.id())
          .flatMap(id -> taskStore.fetchTaskHistory(id)
              .flatMap(historyTask -> convert(activityDefinition, historyTask, List.of()))
              .collectList());
    }
  }

  private Mono<ActivityDefinition> findActivityDefinition(Task task) {
    return Mono.justOrEmpty(task.instantiatesCanonical())
        .flatMap(taskStore::findActivityDefinitionByUrl);
  }
}
