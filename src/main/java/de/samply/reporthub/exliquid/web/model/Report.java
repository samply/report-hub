package de.samply.reporthub.exliquid.web.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record Report(
    OffsetDateTime date,
    int totalNumberOfPatients,
    int totalNumberOfSpecimen,
    List<Stratum> strata) {

  public Report {
    Objects.requireNonNull(date);
    Objects.requireNonNull(strata);
  }

  public record Stratum(String diagnosis, int patientCount, int plasmaCount, int pbmcCount) {

    public Stratum {
      Objects.requireNonNull(diagnosis);
    }
  }
}
