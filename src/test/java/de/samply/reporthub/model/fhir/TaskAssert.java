package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.model.fhir.Task.Output;
import java.util.Objects;
import java.util.function.Predicate;
import org.assertj.core.api.AbstractAssert;

public class TaskAssert extends AbstractAssert<TaskAssert, Task> {

  TaskAssert(Task actual) {
    super(actual, TaskAssert.class);
  }

  public TaskAssert hasStatus(Code status) {
    isNotNull();
    if (!Objects.equals(actual.status(), status)) {
      failWithMessage("Expected task's status to be <%s> but was <%s>", status, actual.status());
    }
    return this;
  }

  public TaskAssert containsOutput(Predicate<CodeableConcept> typePredicate,
      Element expectedValue) {
    isNotNull();
    assertThat(actual.findOutput(typePredicate)).map(Output::value).as("task output value")
        .contains(expectedValue);
    return this;
  }
}
