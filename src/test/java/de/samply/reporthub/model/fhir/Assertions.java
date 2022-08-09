package de.samply.reporthub.model.fhir;

public interface Assertions {

  static TaskAssert assertThat(Task actual) {
    return new TaskAssert(actual);
  }
}
