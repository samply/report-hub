package de.samply.reporthub.web.controller;

import de.samply.reporthub.service.TaskStore;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ActivityDefinitionController {

  private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionController.class);

  private final TaskStore taskStore;

  public ActivityDefinitionController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  @GetMapping("activity-definition/{id}")
  public String activityDefinition(@PathVariable("id") String id, Model model) {
    model.addAttribute("activityDefinition", taskStore.fetchActivityDefinition(id));
    return "activity-definition";
  }
}
