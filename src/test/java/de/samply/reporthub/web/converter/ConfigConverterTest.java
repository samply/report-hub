package de.samply.reporthub.web.converter;

import static org.junit.jupiter.api.Assertions.*;

import de.samply.reporthub.model.fhir.CapabilityStatement;
import de.samply.reporthub.model.fhir.CapabilityStatement.Software;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ConfigConverterTest {

  @Test
  void software() {
    var software = ConfigConverter.software(CapabilityStatement.builder().build());

    assertEquals("Unknown", software);
  }

  @Test
  void software_nameOnly() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder("name-103620").build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertEquals("name-103620", software);
  }

  @Test
  void software_nameAndVersion() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder("name-103620").withVersion("version-103703").build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertEquals("name-103620 (version-103703)", software);
  }

  @Test
  void software_nameAndReleaseDate() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder("name-103620")
            .withReleaseDate(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
            .build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertEquals("name-103620 (1970-01-01)", software);
  }

  @Test
  void software_nameVersionAndReleaseDate() {
    var capabilityStatement = CapabilityStatement.builder()
        .withSoftware(Software.builder("name-103620")
            .withVersion("version-103703")
            .withReleaseDate(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
            .build())
        .build();

    var software = ConfigConverter.software(capabilityStatement);

    assertEquals("name-103620 (version-103703) (1970-01-01)", software);
  }
}
