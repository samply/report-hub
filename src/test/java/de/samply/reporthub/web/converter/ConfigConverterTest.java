package de.samply.reporthub.web.converter;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.model.fhir.CapabilityStatement;
import de.samply.reporthub.model.fhir.CapabilityStatement.Software;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ConfigConverterTest {

  private static final String SOFTWARE_NAME = "software-name-103620";
  private static final String SOFTWARE_VERSION = "software-version-103703";
  private static final OffsetDateTime SOFTWARE_RELEASE_DATE =
      OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void software() {
    var software = ConfigConverter.software(CapabilityStatement.builder().build());

    assertThat(software).isEqualTo("Unknown");
  }

  @Test
  void software_nameOnly() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder(SOFTWARE_NAME).build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertThat(software).isEqualTo(SOFTWARE_NAME);
  }

  @Test
  void software_nameAndVersion() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder(SOFTWARE_NAME).withVersion(SOFTWARE_VERSION).build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertThat(software).isEqualTo("%s (%s)", SOFTWARE_NAME, SOFTWARE_VERSION);
  }

  @Test
  void software_nameAndReleaseDate() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder(SOFTWARE_NAME)
            .withReleaseDate(SOFTWARE_RELEASE_DATE)
            .build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertThat(software).isEqualTo("%s (1970-01-01)", SOFTWARE_NAME);
  }

  @Test
  void software_nameVersionAndReleaseDate() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder(SOFTWARE_NAME)
            .withVersion(SOFTWARE_VERSION)
            .withReleaseDate(SOFTWARE_RELEASE_DATE)
            .build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertThat(software).isEqualTo("%s (%s) (1970-01-01)", SOFTWARE_NAME, SOFTWARE_VERSION);
  }
}
