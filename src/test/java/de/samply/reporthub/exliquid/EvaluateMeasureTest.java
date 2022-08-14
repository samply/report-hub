package de.samply.reporthub.exliquid;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.ClasspathIo;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Code;
import de.samply.reporthub.model.fhir.CodeableConcept;
import de.samply.reporthub.model.fhir.Measure;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class EvaluateMeasureTest {

  private static final String POPULATION_BASIS =
      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-populationBasis";
  private static final String EXLIQUID_MEASURE_GROUP =
      "https://dktk.dkfz.de/fhir/CodeSystem/exliquid-measure-group";

  private static final Predicate<CodeableConcept> PATIENT = CodeableConcept.containsCoding(
      EXLIQUID_MEASURE_GROUP, "patient");
  private static final Predicate<CodeableConcept> SPECIMEN = CodeableConcept.containsCoding(
      EXLIQUID_MEASURE_GROUP, "specimen");

  @Test
  void readMeasure() {
    var json = ClasspathIo.slurp("exliquid/Measure-dashboard.json").block();

    var measure = Util.parseJson(json, Measure.class).block();

    assertThat(measure).isNotNull();
    assertThat(measure.name()).contains("exliquid");
    assertThat(measure.title()).contains("EXLIQUID");
    assertThat(measure.findGroup(PATIENT)
        .flatMap(g -> g.findExtension(POPULATION_BASIS))
        .flatMap(e -> e.castValue(Code.class))
        .flatMap(Code::value)).contains("boolean");
    assertThat(measure.findGroup(SPECIMEN)
        .flatMap(g -> g.findExtension(POPULATION_BASIS))
        .flatMap(e -> e.castValue(Code.class))
        .flatMap(Code::value)).contains("Specimen");
  }
}
