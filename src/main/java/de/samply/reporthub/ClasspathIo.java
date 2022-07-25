package de.samply.reporthub;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Utility methods for working with the classpath.
 */
public interface ClasspathIo {

  Logger logger = LoggerFactory.getLogger(ClasspathIo.class);

  /**
   * Reads the content of resource with {@code name} into a string.
   *
   * @param name the name of the resource to read relative to this class
   * @return either the content of the resource or an error message
   */
  static Mono<String> slurp(String name) {
    try (InputStream in = ClasspathIo.class.getResourceAsStream(name)) {
      if (in == null) {
        logger.error("file `{}` not found in classpath", name);
        return Mono.error(new Exception("file `%s` not found in classpath".formatted(name)));
      } else {
        logger.debug("read file `{}` from classpath", name);
        return Mono.just(new String(in.readAllBytes(), UTF_8));
      }
    } catch (IOException e) {
      logger.error("error while reading the file `{}` from classpath", name, e);
      return Mono.error(
          new Exception("error while reading the file `%s` from classpath".formatted(name)));
    }
  }
}
