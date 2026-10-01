package io.teampulse;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

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
    DeterministicTenantContextProvider tenantContextProvider() {
        return new DeterministicTenantContextProvider(
            new TenantContext(TEST_TENANT_REFERENCE)
        );
    }

    public static class DeterministicTenantContextProvider implements TenantContextProvider {

        private final TenantContext tenantContext;
        private final AtomicInteger calls = new AtomicInteger();

        public DeterministicTenantContextProvider(TenantContext tenantContext) {
            this.tenantContext = tenantContext;
        }

        @Override
        public TenantContext current() {
            calls.incrementAndGet();
            return tenantContext;
        }

        public int calls() {
            return calls.get();
        }

        public void resetCalls() {
            calls.set(0);
        }
    }
}
