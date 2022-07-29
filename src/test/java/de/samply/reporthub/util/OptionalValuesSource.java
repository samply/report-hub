package de.samply.reporthub.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.params.provider.ArgumentsSource;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(OptionalValuesArgumentsProvider.class)
public @interface OptionalValuesSource {

  Value[] value() default {};

  @interface Value {

    String[] value() default {};
  }
}
