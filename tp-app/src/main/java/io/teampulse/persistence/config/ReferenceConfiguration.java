package io.teampulse.persistence.config;

import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReferenceConfiguration {

    @Bean
    Clock referenceClock() {
        return Clock.systemUTC();
    }

    @Bean
    ReferenceFactory referenceFactory(Clock referenceClock) {
        return new MonotonicReferenceFactory(referenceClock);
    }
}
