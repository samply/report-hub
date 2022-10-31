package de.samply.reporthub;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class ReportHubApplication {

  private static final int TWO_MEGA_BYTE = 2 * 1024 * 1024;

  @Value("${app.beam.appId}")
  private String beamAppId;

  @Value("${app.beam.secret}")
  private String beamSecret;

  public static void main(String[] args) throws InterruptedException {
    Thread.sleep(35000);
    SpringApplication.run(ReportHubApplication.class, args);
  }

  @Bean
  public WebClient beamProxy(@Value("${app.beam.proxy.baseUrl}") String baseUrl) {
    return WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Accept", APPLICATION_JSON_VALUE)
        .defaultHeader("Authorization", "ApiKey %s %s".formatted(beamAppId, beamSecret))
        .build();
  }

  @Bean
  public WebClient taskStoreClient(@Value("${app.taskStore.baseUrl}") String baseUrl,
      ObjectMapper mapper) {
    return storeClient(baseUrl, mapper);
  }

  @Bean
  public WebClient dataStoreClient(@Value("${app.dataStore.baseUrl}") String baseUrl,
      ObjectMapper mapper) {
    return storeClient(baseUrl, mapper);
  }

  private static WebClient storeClient(String baseUrl, ObjectMapper mapper) {
    return WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Accept", "application/fhir+json")
        .codecs(configurer -> {
          var codecs = configurer.defaultCodecs();
          codecs.maxInMemorySize(TWO_MEGA_BYTE);
          codecs.jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
          codecs.jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
        })
        .build();
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
