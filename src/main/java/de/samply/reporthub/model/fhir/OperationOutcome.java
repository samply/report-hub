package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.OperationOutcome.Builder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record OperationOutcome(
    Optional<String> id,
    Optional<Meta> meta,
    List<Issue> issue) implements Resource {

  public OperationOutcome {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(issue);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private List<Issue> issues;

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withIssues(List<Issue> issues) {
      this.issues = issues;
      return this;
    }

    public OperationOutcome build() {
      return new OperationOutcome(
          Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Util.copyOfNullable(issues));
    }
  }

  @JsonDeserialize(builder = Issue.Builder.class)
  public record Issue(
      Code severity,
      Code code,
      Optional<CodeableConcept> details,
      Optional<String> diagnostics,
      List<String> expression) implements BackboneElement {

    public Issue {
      Objects.requireNonNull(severity);
      Objects.requireNonNull(code);
      Objects.requireNonNull(details);
      Objects.requireNonNull(diagnostics);
      Objects.requireNonNull(expression);
    }

    public static class Builder {

      private Code severity;
      private Code code;
      private CodeableConcept details;
      private String diagnostics;
      private List<String> expression;

      public Builder withSeverity(Code severity) {
        this.severity = Objects.requireNonNull(severity);
        return this;
      }

      public Builder withCode(Code code) {
        this.code = Objects.requireNonNull(code);
        return this;
      }

      public Builder withDetails(CodeableConcept details) {
        this.details = Objects.requireNonNull(details);
        return this;
      }

      public Builder withDiagnostics(String diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics);
        return this;
      }

      public Builder withExpression(List<String> expression) {
        this.expression = expression;
        return this;
      }

      public Issue build() {
        return new Issue(
            severity,
            code,
            Optional.ofNullable(details),
            Optional.ofNullable(diagnostics),
            Util.copyOfNullable(expression)
        );
      }
    }
  }
}
