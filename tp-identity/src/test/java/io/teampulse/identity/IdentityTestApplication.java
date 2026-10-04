package io.teampulse.identity;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@TestConfiguration
@SpringBootApplication
public class IdentityTestApplication {

    private static final TenantContext TEST_TENANT = new TenantContext("ORG-2026-0908-00000ZA7B900");

    @Bean
    ReferenceFactory referenceFactory() {
        return new MonotonicReferenceFactory(Clock.systemUTC());
    }

    @Bean
    TenantContextProvider testTenantContextProvider() {
        return () -> TEST_TENANT;
    }
}
