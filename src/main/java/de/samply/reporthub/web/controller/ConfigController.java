package de.samply.reporthub.web.controller;

import de.samply.reporthub.service.DataStore;
import de.samply.reporthub.service.Store;
import de.samply.reporthub.service.TaskStore;
import de.samply.reporthub.web.converter.ConfigConverter;
import de.samply.reporthub.web.model.StoreConfig;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
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
    this.taskStoreBaseUrl = taskStoreBaseUrl;
    this.dataStore = dataStore;
    this.dataStoreBaseUrl = dataStoreBaseUrl;
  }

  @GetMapping("config")
  public String config(Model model) {
    model.addAttribute("taskStore", storeConfig(taskStoreBaseUrl, taskStore));
    model.addAttribute("dataStore", storeConfig(dataStoreBaseUrl, dataStore));
    return "config";
  }

  public static Mono<StoreConfig> storeConfig(String baseUrl, Store store) {
    return store.fetchMetadata()
        .map(capabilityStatement -> ConfigConverter.convert(baseUrl, capabilityStatement))
        .onErrorReturn(ConfigConverter.errorConfig(baseUrl));
  }
}
