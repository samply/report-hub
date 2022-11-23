package de.samply.reporthub.web.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.seeOther;

import de.samply.reporthub.service.EvaluateMeasureMessageService;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class BeamController {

  private static final Logger logger = LoggerFactory.getLogger(BeamController.class);

  private final EvaluateMeasureMessageService service;

  public BeamController(EvaluateMeasureMessageService service) {
    this.service = Objects.requireNonNull(service);
  }

  @Bean
  public RouterFunction<ServerResponse> beamRouter() {
    return route(GET("beam"), this::handle)
        .andNest(path("beam/poller"), route(POST("start"), this::start)
            .andRoute(POST("stop"), this::stop));
  }

  public Mono<ServerResponse> handle(ServerRequest request) {
    logger.debug("Request Beam page");
    return model().flatMap(model -> ok().render("beam", model));
  }

  private Mono<ServerResponse> start(ServerRequest request) {
    logger.debug("Start Beam poller");
    service.restart();
    return seeOther(request.uriBuilder().pathSegment("..", "..").build()).build();
  }

  private Mono<ServerResponse> stop(ServerRequest request) {
    logger.debug("Stop Beam poller");
    service.stop();
    return seeOther(request.uriBuilder().pathSegment("..", "..").build()).build();
  }

  Mono<Map<String, Object>> model() {
    return Mono.just(Map.of("poller", service));
  }
}
