package de.samply.reporthub.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

public class OptionalValuesArgumentsProvider implements ArgumentsProvider,
    AnnotationConsumer<OptionalValuesSource> {

  private List<Arguments> arguments;

  @Override
  public void accept(OptionalValuesSource source) {
    arguments = Stream.of(source.value())
        .map(value -> Arguments.of(Stream.of(value.value())
            .map(s -> s.isEmpty() ? Optional.empty() : Optional.of(s))
            .toArray()))
        .toList();
  }

  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
    return arguments.stream();
  }
}
