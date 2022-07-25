package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.model.fhir.Expression.Builder;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(Include.NON_EMPTY)
@JsonDeserialize(builder = Builder.class)
public record Expression(
    Optional<String> description,
    Optional<String> name,
    Code language,
    Optional<String> expression,
    Optional<String> reference) implements Element {

  public Expression {
    Objects.requireNonNull(language);
  }

  public static Builder builder(Code language) {
    return new Builder(language);
  }

  public static class Builder {

    private String description;
    private String name;
    private Code language;
    private String expression;
    private String reference;

    public Builder() {
    }

    private Builder(Code language) {
      this.language = Objects.requireNonNull(language);
    }

    public Builder withDescription(String description) {
      this.description = Objects.requireNonNull(description);
      return this;
    }

    public Builder withName(String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    public Builder withLanguage(Code language) {
      this.language = Objects.requireNonNull(language);
      return this;
    }

    public Builder withExpression(String expression) {
      this.expression = Objects.requireNonNull(expression);
      return this;
    }

    public Builder withReference(String reference) {
      this.reference = Objects.requireNonNull(reference);
      return this;
    }

    public Expression build() {
      return new Expression(
          Optional.ofNullable(description),
          Optional.ofNullable(name),
          language,
          Optional.ofNullable(expression),
          Optional.ofNullable(reference)
      );
    }
  }
}
