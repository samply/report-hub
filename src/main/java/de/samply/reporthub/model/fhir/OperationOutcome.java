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
    List<Issue> issue) implements Resource<OperationOutcome> {

  public OperationOutcome {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(issue);
  }

  @Override
  public OperationOutcome withId(String id) {
    return new Builder(this).withId(id).build();
  }

  public static OperationOutcome issue(Issue issue) {
    return OperationOutcome.builder().withIssue(List.of(issue)).build();
  }

  public static OperationOutcome empty() {
    return OperationOutcome.builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private List<Issue> issue;

    public Builder() {
    }

    private Builder(OperationOutcome outcome) {
      id = outcome.id.orElse(null);
      meta = outcome.meta.orElse(null);
      issue = outcome.issue;
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withIssue(List<Issue> issue) {
      this.issue = issue;
      return this;
    }

    public OperationOutcome build() {
      return new OperationOutcome(
          Optional.ofNullable(id),
          Optional.ofNullable(meta),
          Util.copyOfNullable(issue));
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

    public static Builder builder(Code severity, Code code) {
      return new Builder(severity, code);
    }

    public static class Builder {

      private Code severity;
      private Code code;
      private CodeableConcept details;
      private String diagnostics;
      private List<String> expression;

      public Builder() {
      }

      public Builder(Code severity, Code code) {
        this.severity = Objects.requireNonNull(severity);
        this.code = Objects.requireNonNull(code);
      }

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
