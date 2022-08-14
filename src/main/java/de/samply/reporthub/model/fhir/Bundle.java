package de.samply.reporthub.model.fhir;

import static de.samply.reporthub.model.fhir.BundleType.MESSAGE;
import static de.samply.reporthub.model.fhir.BundleType.TRANSACTION;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Bundle.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Bundle(
    Optional<String> id,
    Optional<Meta> meta,
    Code type,
    List<Entry> entry) implements Resource<Bundle> {

  public Bundle {
    Objects.requireNonNull(id);
    Objects.requireNonNull(meta);
    Objects.requireNonNull(type, "missing type");
    Objects.requireNonNull(entry);
  }

  @Override
  public Bundle withId(String id) {
    return new Builder(this).withId(id).build();
  }

  /**
   * Returns the resource of the first entry if it has the given {@code type}.
   *
   * @param type the type of the first resource
   * @param <T>  the type of the resource
   * @return an {@code Optional} of the found first resource or an empty {@code Optional} if no
   * first resource with {@code type} was found
   */
  public <T extends Resource<T>> Optional<T> firstResourceAs(Class<T> type) {
    return entry.isEmpty() ? Optional.empty() : entry.get(0).resourceAs(type);
  }

  public Optional<Entry> findFirstEntry(Predicate<Entry> predicate) {
    return entry.stream().filter(predicate).findFirst();
  }

  public Optional<? extends Resource<?>> resolveResource(Reference reference) {
    return reference.reference()
        .flatMap(ref -> findFirstEntry(e -> e.fullUrl.stream().anyMatch(uri -> uri.hasValue(ref))))
        .flatMap(e -> e.resource);
  }

  public <T extends Resource<T>> Optional<T> resolveResource(Class<T> type, Reference reference) {
    return reference.reference()
        .flatMap(ref -> findFirstEntry(e -> e.fullUrl.stream().anyMatch(uri -> uri.hasValue(ref))))
        .flatMap(e -> e.resourceAs(type));
  }

  public <T extends Resource<T>> Stream<T> resourcesAs(Class<T> type) {
    return entry.stream().map(e -> e.resourceAs(type)).flatMap(Optional::stream);
  }

  public static <T extends Resource<T>> Predicate<Bundle> hasFirstResource(Class<T> type,
      Predicate<T> predicate) {
    return bundle -> bundle.firstResourceAs(type).stream().anyMatch(predicate);
  }

  public static Builder message() {
    return new Builder(MESSAGE.code());
  }

  public static Builder transaction() {
    return new Builder(TRANSACTION.code());
  }

  public static Builder builder(Code type) {
    return new Builder(type);
  }

  public <T extends Resource<T>> Optional<Bundle> mapFirstResource(Class<T> type,
      Function<? super T, ? extends T> mapper) {
    Objects.requireNonNull(mapper);
    return entry.stream().findFirst()
        .flatMap(firstEntry -> firstEntry.resourceAs(type).map(resource -> {
          var newEntry = new ArrayList<Entry>();
          newEntry.add(firstEntry.withResource(mapper.apply(resource)));
          newEntry.addAll(entry.stream().skip(1).toList());
          return new Builder(this).withEntry(newEntry).build();
        }));
  }

  public static class Builder {

    private String id;
    private Meta meta;
    private Code type;
    private List<Entry> entry;

    public Builder() {
    }

    private Builder(Code type) {
      this.type = Objects.requireNonNull(type);
    }

    private Builder(Bundle bundle) {
      id = bundle.id.orElse(null);
      meta = bundle.meta.orElse(null);
      type = bundle.type;
      entry = bundle.entry;
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    public Builder withMeta(Meta meta) {
      this.meta = Objects.requireNonNull(meta);
      return this;
    }

    public Builder withType(Code type) {
      this.type = Objects.requireNonNull(type);
      return this;
    }

    public Builder withEntry(List<Entry> entry) {
      this.entry = entry;
      return this;
    }

    public Bundle build() {
      return new Bundle(
          Optional.ofNullable(id),
          Optional.ofNullable(meta),
          type,
          Util.copyOfNullable(entry));
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonDeserialize(builder = Entry.Builder.class)
  public record Entry(
      Optional<Uri> fullUrl,
      Optional<Resource<?>> resource,
      Optional<Request> request,
      Optional<Response> response) implements BackboneElement {

    public Entry {
      Objects.requireNonNull(fullUrl);
      Objects.requireNonNull(resource);
      Objects.requireNonNull(request);
      Objects.requireNonNull(response);
    }

    public Entry withResource(Resource<?> resource) {
      return new Builder(this).withResource(resource).build();
    }

    public <T extends Resource<T>> Optional<T> resourceAs(Class<T> type) {
      return resource.flatMap(r -> r.cast(type));
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {

      private Uri fullUrl;
      private Resource<?> resource;
      private Request request;
      private Response response;

      public Builder() {
      }

      private Builder(Entry entry) {
        fullUrl = entry.fullUrl.orElse(null);
        resource = entry.resource.orElse(null);
        request = entry.request.orElse(null);
        response = entry.response.orElse(null);
      }

      public Builder withFullUrl(Uri fullUrl) {
        this.fullUrl = Objects.requireNonNull(fullUrl);
        return this;
      }

      public Builder withResource(Resource<?> resource) {
        this.resource = Objects.requireNonNull(resource);
        return this;
      }

      public Builder withRequest(Request request) {
        this.request = Objects.requireNonNull(request);
        return this;
      }

      public Builder withResponse(Response response) {
        this.response = Objects.requireNonNull(response);
        return this;
      }

      public Entry build() {
        return new Entry(
            Optional.ofNullable(fullUrl),
            Optional.ofNullable(resource),
            Optional.ofNullable(request),
            Optional.ofNullable(response));
      }
    }

    @JsonInclude(Include.NON_EMPTY)
    @JsonDeserialize(builder = Request.Builder.class)
    public record Request(
        Code method,
        String url,
        Optional<String> ifNoneExist) implements BackboneElement {

      public Request {
        Objects.requireNonNull(method);
        Objects.requireNonNull(url);
        Objects.requireNonNull(ifNoneExist);
      }

      public static Builder builder() {
        return new Builder();
      }

      public static class Builder {

        private Code method;
        private String url;
        private String ifNoneExist;

        public Builder withMethod(Code method) {
          this.method = Objects.requireNonNull(method);
          return this;
        }

        public Builder withUrl(String url) {
          this.url = Objects.requireNonNull(url);
          return this;
        }

        public Builder withIfNoneExist(String ifNoneExist) {
          this.ifNoneExist = Objects.requireNonNull(ifNoneExist);
          return this;
        }

        public Request build() {
          return new Request(method, url, Optional.ofNullable(ifNoneExist));
        }
      }
    }

    @JsonInclude(Include.NON_EMPTY)
    @JsonDeserialize(builder = Response.Builder.class)
    public record Response(String status, Optional<String> location) implements BackboneElement {

      public Response {
        Objects.requireNonNull(status);
        Objects.requireNonNull(location);
      }

      public static Builder builder() {
        return new Builder();
      }

      public static class Builder {

        private String status;
        private String location;

        public Builder withStatus(String status) {
          this.status = Objects.requireNonNull(status);
          return this;
        }

        public Builder withLocation(String location) {
          this.location = Objects.requireNonNull(location);
          return this;
        }

        public Response build() {
          return new Response(status, Optional.ofNullable(location));
        }
      }
    }
  }
}
