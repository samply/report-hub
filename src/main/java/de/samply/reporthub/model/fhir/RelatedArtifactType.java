package de.samply.reporthub.model.fhir;

import java.util.function.Predicate;

/**
 * Codes of the CodeSystem {@code http://hl7.org/fhir/related-artifact-type}.
 */
public enum RelatedArtifactType implements Predicate<Code> {

  DOCUMENTATION("documentation"),
  JUSTIFICATION("justification"),
  CITATION("citation"),
  PREDECESSOR("predecessor"),
  SUCCESSOR("successor"),
  DERIVED_FROM("derived-from"),
  DEPENDS_ON("depends-on"),
  COMPOSED_OF("composed-of");

  private final String code;

  RelatedArtifactType(String code) {
    this.code = code;
  }

  public Code code() {
    return Code.valueOf(code);
  }

  @Override
  public boolean test(Code code) {
    return code.hasValue(this.code);
  }

  @Override
  public String toString() {
    return code;
  }
}
