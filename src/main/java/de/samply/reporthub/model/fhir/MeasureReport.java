package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.MeasureReport.Builder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record MeasureReport(
    Optional<String> id,
    Code status,
    Code type,
    String measure,
    Optional<OffsetDateTime> date,
    List<Group> group) implements Resource {

  public MeasureReport {
    Objects.requireNonNull(id);
    Objects.requireNonNull(status);
    Objects.requireNonNull(type);
    Objects.requireNonNull(measure);
    Objects.requireNonNull(date);
    Objects.requireNonNull(group);
  }

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

  public static Builder builder(Code status, Code type, String measure) {
    return new Builder(status, type, measure);
  }

  public static class Builder {

    private String id;
    private Code status;
    private Code type;
    private String measure;
    private OffsetDateTime date;
    private List<Group> group;

    public Builder() {
    }

    private Builder(Code status, Code type, String measure) {
      this.status = Objects.requireNonNull(status);
      this.type = Objects.requireNonNull(type);
      this.measure = Objects.requireNonNull(measure);
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withStatus(Code status) {
      this.status = Objects.requireNonNull(status);
      return this;
    }

    public Builder withType(Code type) {
      this.type = Objects.requireNonNull(type);
      return this;
    }

    public Builder withMeasure(String measure) {
      this.measure = Objects.requireNonNull(measure);
      return this;
    }

    public Builder withDate(OffsetDateTime date) {
      this.date = Objects.requireNonNull(date);
      return this;
    }

    public Builder withGroup(List<Group> group) {
      this.group = group;
      return this;
    }

    public MeasureReport build() {
      return new MeasureReport(
          Optional.ofNullable(id),
          status,
          type,
          measure,
          Optional.ofNullable(date),
          Util.copyOfNullable(group));
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonDeserialize(builder = Group.Builder.class)
  public record Group(
      Optional<CodeableConcept> code,
      List<Population> population,
      List<Stratifier> stratifier) implements BackboneElement {

    public Group {
      Objects.requireNonNull(code);
      Objects.requireNonNull(population);
      Objects.requireNonNull(stratifier);
    }

    public Optional<Population> findPopulation(Predicate<CodeableConcept> codePredicate) {
      return population.stream().filter(p -> p.code.stream().anyMatch(codePredicate)).findFirst();
    }

    public Optional<Stratifier> findStratifier(Predicate<CodeableConcept> codePredicate) {
      return stratifier.stream().filter(s -> s.code.stream().anyMatch(codePredicate)).findFirst();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {

      private CodeableConcept code;
      private List<Population> population;
      private List<Stratifier> stratifier;

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
        return new Group(
            Optional.ofNullable(code),
            Util.copyOfNullable(population),
            Util.copyOfNullable(stratifier));
      }
    }

    @JsonInclude(Include.NON_EMPTY)
    @JsonDeserialize(builder = Population.Builder.class)
    public record Population(
        Optional<CodeableConcept> code,
        Optional<Integer> count) implements BackboneElement {

      public Population {
        Objects.requireNonNull(code);
        Objects.requireNonNull(count);
      }

      public static Population of(CodeableConcept code, Integer count) {
        return new Builder().withCode(code).withCount(count).build();
      }

      public static Builder builder() {
        return new Builder();
      }

      public static class Builder {

        private CodeableConcept code;
        private Integer count;

        public Builder withCode(CodeableConcept code) {
          this.code = Objects.requireNonNull(code);
          return this;
        }

        public Builder withCount(Integer count) {
          this.count = Objects.requireNonNull(count);
          return this;
        }

        public Population build() {
          return new Population(Optional.ofNullable(code), Optional.ofNullable(count));
        }
      }
    }

    @JsonInclude(Include.NON_EMPTY)
    @JsonDeserialize(builder = Stratifier.Builder.class)
    public record Stratifier(
        List<CodeableConcept> code,
        List<Stratum> stratum) implements BackboneElement {

      public Stratifier {
        Objects.requireNonNull(code);
        Objects.requireNonNull(stratum);
      }

      public static Builder builder() {
        return new Builder();
      }

      public static class Builder {

        private List<CodeableConcept> code;
        private List<Stratum> stratum;

        public Builder withCode(List<CodeableConcept> code) {
          this.code = code;
          return this;
        }

        public Builder withStratum(List<Stratum> stratum) {
          this.stratum = stratum;
          return this;
        }

        public Stratifier build() {
          return new Stratifier(Util.copyOfNullable(code), Util.copyOfNullable(stratum));
        }
      }

      @JsonInclude(Include.NON_EMPTY)
      @JsonDeserialize(builder = Stratum.Builder.class)
      public record Stratum(
          Optional<CodeableConcept> value,
          List<Component> component,
          List<Population> population) implements BackboneElement {

        public Stratum {
          Objects.requireNonNull(value);
          Objects.requireNonNull(component);
          Objects.requireNonNull(population);
        }

        public Optional<Component> findComponent(Predicate<CodeableConcept> codePredicate) {
          return component.stream().filter(c -> codePredicate.test(c.code)).findFirst();
        }

        public Optional<Population> findPopulation(Predicate<CodeableConcept> codePredicate) {
          return population.stream().filter(p -> p.code.stream().anyMatch(codePredicate))
              .findFirst();
        }

        public static Builder builder() {
          return new Builder();
        }

        public static class Builder {

          private CodeableConcept value;
          private List<Component> component;
          private List<Population> population;

          public Builder withValue(CodeableConcept value) {
            this.value = Objects.requireNonNull(value);
            return this;
          }

          public Builder withComponent(List<Component> component) {
            this.component = component;
            return this;
          }

          public Builder withPopulation(List<Population> population) {
            this.population = population;
            return this;
          }

          public Stratum build() {
            return new Stratum(
                Optional.ofNullable(value),
                Util.copyOfNullable(component),
                Util.copyOfNullable(population));
          }
        }

        public record Component(
            CodeableConcept code,
            CodeableConcept value) implements BackboneElement {

          public Component {
            Objects.requireNonNull(code);
            Objects.requireNonNull(value);
          }

          public static Component of(CodeableConcept code, CodeableConcept value) {
            return new Component(code, value);
          }
        }

        @JsonInclude(Include.NON_EMPTY)
        @JsonDeserialize(builder = Population.Builder.class)
        public record Population(
            Optional<CodeableConcept> code,
            Optional<Integer> count) implements BackboneElement {

          public Population {
            Objects.requireNonNull(code);
            Objects.requireNonNull(count);
          }

          public static Population of(CodeableConcept code, Integer count) {
            return new Population.Builder().withCode(code).withCount(count).build();
          }

          public static class Builder {

            private CodeableConcept code;
            private Integer count;

            public Builder withCode(CodeableConcept code) {
              this.code = Objects.requireNonNull(code);
              return this;
            }

            public Builder withCount(Integer count) {
              this.count = Objects.requireNonNull(count);
              return this;
            }

            public Population build() {
              return new Population(Optional.ofNullable(code), Optional.ofNullable(count));
            }
          }
        }
      }
    }
  }
}
