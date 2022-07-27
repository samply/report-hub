package de.samply.reporthub.exliquid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.reporthub.ClasspathIo;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Extension;
import de.samply.reporthub.model.fhir.Measure;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class EvaluateMeasureTest {

  public static final String POPULATION_BASIS = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-populationBasis";
  public static final String EXLIQUID_MEASURE_GROUP = "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-measure-group";

  public static final Predicate<CodeableConcept> PATIENT = CodeableConcept.containsCoding(
      EXLIQUID_MEASURE_GROUP, "patient");
  public static final Predicate<CodeableConcept> SPECIMEN = CodeableConcept.containsCoding(
      EXLIQUID_MEASURE_GROUP, "specimen");

  @Test
  void readMeasure() throws IOException {
    var json = ClasspathIo.slurp("exliquid/Measure-dashboard.json").block();

    var measure = new ObjectMapper().readValue(json, Measure.class);

    assertEquals(Optional.of("EXLIQUID"), measure.name());
    assertEquals(Optional.of("boolean"), measure.findGroup(PATIENT)
        .flatMap(g -> g.findExtension(POPULATION_BASIS))
        .flatMap(e -> e.castValue(Code.class))
        .flatMap(Code::value));
    assertEquals(Optional.of("Specimen"), measure.findGroup(SPECIMEN)
        .flatMap(g -> g.findExtension(POPULATION_BASIS))
        .flatMap(e -> e.castValue(Code.class))
        .flatMap(Code::value));
  }
}
