package io.teampulse.websupport.autoconfigure;

import io.teampulse.websupport.error.SharedHttpErrorAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(SharedHttpErrorAdvice.class)
public class WebSupportAutoConfiguration {
}
