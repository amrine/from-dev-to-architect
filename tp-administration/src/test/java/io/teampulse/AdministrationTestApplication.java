package io.teampulse;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class AdministrationTestApplication {

    static final String TEST_TENANT_REFERENCE = "ORG-2026-1001-00000ZA7B900";

    @Bean
    Clock testClock() {
        return Clock.systemUTC();
    }

    @Bean
    ReferenceFactory referenceFactory(Clock testClock) {
        return new MonotonicReferenceFactory(testClock);
    }

    @Bean
    TenantContextProvider tenantContextProvider() {
        TenantContext tenantContext = new TenantContext(TEST_TENANT_REFERENCE);
        return () -> tenantContext;
    }
}
