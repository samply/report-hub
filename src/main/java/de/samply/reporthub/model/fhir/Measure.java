package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Measure.Builder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Measure(
    Optional<String> id,
    Optional<String> url,
    Optional<String> name,
    Code status,
    Optional<CodeableConcept> subjectCodeableConcept,
    List<String> library,
    Optional<CodeableConcept> scoring,
    List<Group> group) implements Resource {

  /**
   * Finds the {@link Group} where {@code codePredicate} matches on {@link Group#code}.
   *
   * @param codePredicate the predicate to use
   * @return an {@code Optional} of the found {@link Group} or an empty {@code Optional} if no Group
   * was found
   */
  public Optional<Group> findGroup(Predicate<CodeableConcept> codePredicate) {
    return group.stream().filter(g -> g.code.stream().anyMatch(codePredicate)).findFirst();
  }

  public Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String id;
    private String url;
    private String name;
    private Code status;
    private CodeableConcept subjectCodeableConcept;
    private List<String> library;
    private CodeableConcept scoring;
    private List<Group> group;

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withUrl(String url) {
      this.url = Objects.requireNonNull(url);
      return this;
    }

    public Builder withName(String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    public Builder withStatus(Code status) {
      this.status = Objects.requireNonNull(status);
      return this;
    }

    public Builder withSubjectCodeableConcept(CodeableConcept subjectCodeableConcept) {
      this.subjectCodeableConcept = Objects.requireNonNull(subjectCodeableConcept);
      return this;
    }

    public Builder withLibrary(List<String> library) {
      this.library = library;
      return this;
    }

    public Builder withScoring(CodeableConcept scoring) {
      this.scoring = Objects.requireNonNull(scoring);
      return this;
    }

    public Builder withGroup(List<Group> group) {
      this.group = group;
      return this;
    }

    public Measure build() {
      return new Measure(Optional.ofNullable(id),
          Optional.ofNullable(url),
          Optional.ofNullable(name),
          status,
          Optional.ofNullable(subjectCodeableConcept),
          Util.copyOfNullable(library),
          Optional.ofNullable(scoring),
          Util.copyOfNullable(group));
    }
  }

  /**
   * A group of population criteria for the measure.
   *
   * @param extension
   * @param code       the meaning of the group
   * @param population one or many population criteria
   * @param stratifier one or many stratifier criteria
   */
  @JsonInclude(Include.NON_EMPTY)
  @JsonDeserialize(builder = Group.Builder.class)
  public record Group(
      List<Extension> extension,
      Optional<CodeableConcept> code,
      List<Population> population,
      List<Stratifier> stratifier) {

    /**
     * Finds the {@link Extension} with the given {@code url}.
     *
     * @param url the URL to match
     * @return an {@code Optional} of the found {@link Extension} or an empty {@code Optional} if no
     * Extension was found
     */
    public Optional<Extension> findExtension(String url) {
      return extension.stream()
          .filter(e -> Objects.requireNonNull(url).equals(e.url()))
          .findFirst();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {

      private List<Extension> extension;
      private CodeableConcept code;
      private List<Population> population;
      private List<Stratifier> stratifier;

      public Builder withExtension(List<Extension> extension) {
        this.extension = extension;
        return this;
      }

      public Builder withCode(CodeableConcept code) {
        this.code = Objects.requireNonNull(code);
        return this;
      }

      public Builder withPopulation(List<Population> population) {
        this.population = population;
        return this;
      }

      public Builder withStratifier(List<Stratifier> stratifier) {
        this.stratifier = stratifier;
        return this;
      }

      public Group build() {
        return new Group(Util.copyOfNullable(extension),
            Optional.ofNullable(code),
            Util.copyOfNullable(population),
            Util.copyOfNullable(stratifier));
      }
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonDeserialize(builder = Population.Builder.class)
  public record Population(Optional<CodeableConcept> code, Expression criteria) {

    public static class Builder {

      private CodeableConcept code;
      private Expression criteria;

      public Builder withCode(CodeableConcept code) {
        this.code = Objects.requireNonNull(code);
        return this;
      }

      public Builder withCriteria(Expression criteria) {
        this.criteria = Objects.requireNonNull(criteria);
        return this;
      }

      public Population build() {
        return new Population(Optional.ofNullable(code), criteria);
      }
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonDeserialize(builder = Stratifier.Builder.class)
  public record Stratifier(
      Optional<CodeableConcept> code,
      Expression criteria,
      List<Component> component) {

    public static class Builder {

      private CodeableConcept code;
      private Expression criteria;
      private List<Component> component;

      public Builder withCode(CodeableConcept code) {
        this.code = Objects.requireNonNull(code);
        return this;
      }

      public Builder withCriteria(Expression criteria) {
        this.criteria = Objects.requireNonNull(criteria);
        return this;
      }

      public Builder withComponent(List<Component> component) {
        this.component = component;
        return this;
      }

      public Stratifier build() {
        return new Stratifier(Optional.ofNullable(code), criteria, Util.copyOfNullable(component));
      }
    }

    @JsonInclude(Include.NON_EMPTY)
    @JsonDeserialize(builder = Component.Builder.class)
    public record Component(Optional<CodeableConcept> code, Expression criteria) {

      public static class Builder {

        private CodeableConcept code;
        private Expression criteria;

        public Builder withCode(CodeableConcept code) {
          this.code = Objects.requireNonNull(code);
          return this;
        }

        public Builder withCriteria(Expression criteria) {
          this.criteria = Objects.requireNonNull(criteria);
          return this;
        }

        public Component build() {
          return new Component(Optional.ofNullable(code), criteria);
        }
      }
    }
  }
}
