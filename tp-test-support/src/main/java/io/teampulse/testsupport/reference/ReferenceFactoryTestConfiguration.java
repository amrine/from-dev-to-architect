package io.teampulse.testsupport.reference;

import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration(proxyBeanMethods = false)
public class ReferenceFactoryTestConfiguration {

    private static final Instant TEST_INSTANT = Instant.parse("2026-09-16T08:30:00Z");

    @Bean
    Clock testClock() {
        return Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
    }

    @Bean
    ReferenceFactory referenceFactory(Clock testClock) {
        return new MonotonicReferenceFactory(testClock);
    }
}
