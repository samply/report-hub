package de.samply.reporthub.model.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.samply.reporthub.model.fhir.Task.Input;
import de.samply.reporthub.model.fhir.Task.Output;
import java.util.function.Predicate;
import org.assertj.core.api.AbstractAssert;

@SuppressWarnings("UnusedReturnValue")
public class TaskAssert extends AbstractAssert<TaskAssert, Task> {

  TaskAssert(Task actual) {
    super(actual, TaskAssert.class);
  }

  public TaskAssert hasStatus(TaskStatus status) {
    isNotNull();
    if (!status.test(actual.status())) {
      failWithMessage("Expected task's status to be <%s> but was <%s>", status, actual.status());
    }
    return this;
  }

  public TaskAssert containsInput(Predicate<CodeableConcept> typePredicate,
      Element expectedValue) {
    isNotNull();
    assertThat(actual.findInput(typePredicate)).map(Input::value).as("task input value")
        .contains(expectedValue);
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
