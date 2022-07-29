package de.samply.reporthub;

import static org.springframework.http.MediaType.APPLICATION_JSON;

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

  @Value("${app.beam.proxy.id}")
  private String beamProxyId;

  @Value("${app.beam.appId}")
  private String beamAppId;

  @Value("${app.beam.secret}")
  private String beamSecret;

  public static void main(String[] args) {
    SpringApplication.run(ReportHubApplication.class, args);
  }

  @Bean
  public WebClient beamProxy(@Value("${app.beam.proxy.baseUrl}") String baseUrl) {
    return WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization",
            "ApiKey " + beamAppId + "." + beamProxyId + " " + beamSecret)
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
        .defaultRequest(request -> request.accept(APPLICATION_JSON))
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
