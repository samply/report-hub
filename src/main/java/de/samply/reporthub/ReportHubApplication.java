package de.samply.reporthub;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class ReportHubApplication {

  private static final Logger logger = LoggerFactory.getLogger(ReportHubApplication.class);

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
  public WebClient taskStoreClient(@Value("${app.taskStore.baseUrl}") String baseUrl, ObjectMapper mapper) {
    return WebClient.builder()
        .baseUrl(baseUrl)
        .defaultRequest(request -> request.accept(APPLICATION_JSON))
        .codecs(configurer -> configurer.defaultCodecs()
            .jackson2JsonEncoder(new Jackson2JsonEncoder(mapper)))
        .build();
  }

  @Bean
  public WebClient dataStoreClient(@Value("${app.dataStore.baseUrl}") String baseUrl) {
    logger.info("init data store web client");
    return WebClient.builder()
        .baseUrl(baseUrl)
        .defaultRequest(request -> request.accept(APPLICATION_JSON))
        .build();
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
