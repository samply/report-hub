package de.samply.reporthub.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractAssert;
import org.springframework.web.reactive.function.server.RenderingResponse;

@SuppressWarnings("UnusedReturnValue")
public class RenderingResponseAssert extends
    AbstractAssert<RenderingResponseAssert, RenderingResponse> {

  RenderingResponseAssert(RenderingResponse actual) {
    super(actual, RenderingResponseAssert.class);
  }

  public RenderingResponseAssert hasName(String name) {
    isNotNull();
    if (!Objects.equals(actual.name(), name)) {
      failWithMessage("Expected rendering response's name to be <%s> but was <%s>", name,
          actual.name());
    }
    return this;
  }

  public RenderingResponseAssert containsModelEntry(String key, Object value) {
    isNotNull();
    assertThat(actual.model()).containsEntry(key, value);
    return this;
  }

  public <T> RenderingResponseAssert hasModelEntrySatisfying(String key, Class<T> type,
      Consumer<? super T> valueRequirements) {
    isNotNull();
    assertThat(actual.model()).hasEntrySatisfying(key,
        value -> valueRequirements.accept(type.cast(value)));
    return this;
  }
}
