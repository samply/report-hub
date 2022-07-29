package de.samply.reporthub.web.controller;

import java.net.URI;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RenderingResponse;
import org.springframework.web.reactive.function.server.ServerResponse;

public class ServerResponseAssert extends AbstractAssert<ServerResponseAssert, ServerResponse> {

  private ServerResponseAssert(ServerResponse actual) {
    super(actual, ServerResponseAssert.class);
  }

  public static ServerResponseAssert assertThat(ServerResponse actual) {
    return new ServerResponseAssert(actual);
  }

  public ServerResponseAssert hasStatusCode(HttpStatus statusCode) {
    isNotNull();
    if (!Objects.equals(actual.statusCode(), statusCode)) {
      failWithMessage("Expected response's status code to be <%s> but was <%s>", statusCode,
          actual.statusCode());
    }
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public ServerResponseAssert hasLocation(URI location) {
    isNotNull();
    if (!Objects.equals(actual.headers().getLocation(), location)) {
      failWithMessage("Expected response's location header to be <%s> but was <%s>", location,
          actual.headers().getLocation());
    }
    return this;
  }

  public RenderingResponseAssert isRendering() {
    objects.assertIsInstanceOf(info, actual, RenderingResponse.class);
    return new RenderingResponseAssert((RenderingResponse) actual);
  }
}
