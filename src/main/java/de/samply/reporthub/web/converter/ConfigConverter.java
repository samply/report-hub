package de.samply.reporthub.web.converter;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import de.samply.reporthub.model.fhir.CapabilityStatement;
import de.samply.reporthub.web.model.StoreConfig;

public interface ConfigConverter {

  String UNKNOWN_SOFTWARE = "Unknown";

  static StoreConfig convert(String baseUrl, CapabilityStatement capabilityStatement) {
    return new StoreConfig(baseUrl, software(capabilityStatement), "OK");
  }

  static String software(CapabilityStatement capabilityStatement) {
    return capabilityStatement.software()
        .map(software -> software.version()
            .map(version -> software.releaseDate()
                .map(releaseDate -> "%s (%s) (%s)".formatted(software.name(), version,
                    releaseDate.format(ISO_LOCAL_DATE)))
                .orElse("%s (%s)".formatted(software.name(), version)))
            .orElse(software.releaseDate()
                .map(releaseDate -> "%s (%s)".formatted(software.name(),
                    releaseDate.format(ISO_LOCAL_DATE)))
                .orElse(software.name())))
        .orElse(UNKNOWN_SOFTWARE);
  }

  static StoreConfig errorConfig(String baseUrl) {
    return new StoreConfig(baseUrl, UNKNOWN_SOFTWARE, "ERROR");
  }
}
