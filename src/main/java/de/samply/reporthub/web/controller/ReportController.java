package de.samply.reporthub.web.controller;

import de.samply.reporthub.service.TaskStore;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {

  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

  private final TaskStore taskStore;

  public ReportController(TaskStore taskStore) {
    this.taskStore = Objects.requireNonNull(taskStore);
  }

  @GetMapping("report/{id}")
  public String index(@PathVariable("id") String id, Model model) {
    model.addAttribute("report", taskStore.fetchMeasureReport(id));
    return "report";
  }
}
