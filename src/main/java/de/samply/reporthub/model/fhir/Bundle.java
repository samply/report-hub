package de.samply.reporthub.model.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.reporthub.Util;
import de.samply.reporthub.model.fhir.Bundle.Builder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use = Id.NAME, property = "resourceType")
@JsonDeserialize(builder = Builder.class)
public record Bundle(Optional<String> id, Code type, List<Entry> entry) implements Resource {

  public Bundle {
    Objects.requireNonNull(id);
    Objects.requireNonNull(type);
    Objects.requireNonNull(entry);
  }

  public <T extends Resource> Stream<T> resourcesAs(Class<T> type) {
    return entry.stream().map(e -> e.resourceAs(type)).flatMap(Optional::stream);
  }

  public static Builder builder(Code type) {
    return new Builder(type);
  }

  public static class Builder {

    private String id;
    private Code type;
    private List<Entry> entry;

    public Builder() {
    }

    private Builder(Code type) {
      this.type = Objects.requireNonNull(type);
    }

    public Builder withId(String id) {
      this.id = Objects.requireNonNull(id);
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
      return new Bundle(Optional.ofNullable(id), type, Util.copyOfNullable(entry));
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonDeserialize(builder = Entry.Builder.class)
  public record Entry(
      Optional<Resource> resource,
      Optional<Request> request,
      Optional<Response> response) {

    public Entry {
      Objects.requireNonNull(resource);
      Objects.requireNonNull(request);
      Objects.requireNonNull(response);
    }

    public <T extends Resource> Optional<T> resourceAs(Class<T> type) {
      return resource.flatMap(r -> r.cast(type));
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {

      private Resource resource;
      private Request request;
      private Response response;

      public Builder withResource(Resource resource) {
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
            Optional.ofNullable(resource),
            Optional.ofNullable(request),
            Optional.ofNullable(response));
      }
    }

    @JsonInclude(Include.NON_EMPTY)
    @JsonDeserialize(builder = Request.Builder.class)
    public record Request(Code method, String url, Optional<String> ifNoneExist) {

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
    public record Response(String status, Optional<String> location) {

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
