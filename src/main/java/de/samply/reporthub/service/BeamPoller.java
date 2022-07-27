package de.samply.reporthub.service;

import static de.samply.reporthub.model.fhir.TaskStatus.REQUESTED;

import de.samply.reporthub.Util;
import de.samply.reporthub.component.BeamClient;
import de.samply.reporthub.model.BeamTask;
import de.samply.reporthub.model.fhir.Task;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * A Service constantly polling a beam proxy.
 */
@Service
public class BeamPoller {

  private static final Logger logger = LoggerFactory.getLogger(BeamPoller.class);

  private final BeamClient proxy;
  private final TaskStore taskStore;
  private final Executor executor;

  private volatile boolean running;

  public BeamPoller(BeamClient proxy, TaskStore taskStore, Executor executor) {
    this.proxy = proxy;
    this.taskStore = taskStore;
    this.executor = executor;
  }

  public void start() {
    running = true;
    executor.execute(() -> {
      while (running) {
        proxy.retrieveTasks()
            .flatMap(BeamPoller::convert)
            .flatMap(taskStore::createBeamTask)
            .collectList()
            .block();
      }
    });
  }

  public void stop() {
    running = false;
  }

  static Mono<Task> convert(BeamTask beamTask) {
    return Mono.just(Task.builder(REQUESTED.code())
        .withIdentifier(List.of(Util.beamTaskIdentifier(beamTask.id())))
        .build());
  }
}
