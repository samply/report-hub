package de.samply.reporthub.web.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import de.samply.reporthub.service.DataStore;
import de.samply.reporthub.service.Store;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.util.Monos;
import de.samply.reporthub.web.converter.ConfigConverter;
import de.samply.reporthub.web.model.StoreConfig;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ConfigController {

  private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

  private final TaskStore taskStore;
  private final String taskStoreBaseUrl;
  private final DataStore dataStore;
  private final String dataStoreBaseUrl;

  public ConfigController(
      TaskStore taskStore,
      @Value("${app.taskStore.baseUrl}") String taskStoreBaseUrl,
      DataStore dataStore,
      @Value("${app.dataStore.baseUrl}") String dataStoreBaseUrl) {
    this.taskStore = Objects.requireNonNull(taskStore);
    this.taskStoreBaseUrl = Objects.requireNonNull(taskStoreBaseUrl);
    this.dataStore = Objects.requireNonNull(dataStore);
    this.dataStoreBaseUrl = Objects.requireNonNull(dataStoreBaseUrl);
  }

  @Bean
  public RouterFunction<ServerResponse> configRouter() {
    return route(GET("config"), this::handle);
  }

  public Mono<ServerResponse> handle(ServerRequest request) {
    logger.debug("Request config page");
    return model().flatMap(model -> ok().render("config", model));
  }

  Mono<Map<String, Object>> model() {
    return Monos.map(storeConfig(taskStoreBaseUrl, taskStore),
        storeConfig(dataStoreBaseUrl, dataStore),
        (taskStore, dataStore) -> Map.of("taskStore", taskStore, "dataStore", dataStore));
  }

  public static Mono<StoreConfig> storeConfig(String baseUrl, Store store) {
    return store.fetchMetadata()
        .map(capabilityStatement -> ConfigConverter.convert(baseUrl, capabilityStatement))
        .onErrorReturn(ConfigConverter.errorConfig(baseUrl));
  }
}
