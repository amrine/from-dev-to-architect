package io.teampulse.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration
@SpringBootApplication
public class TeamTestApplication {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-16T08:30:00Z");
    private static final TenantContext TEST_TENANT =
        new TenantContext("ORG-2026-0916-00000ZA7B950");

    @Bean
    Clock teamClock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }

    @Bean
    ReferenceFactory referenceFactory(Clock teamClock) {
        return new MonotonicReferenceFactory(teamClock);
    }

    @Bean
    DeterministicTenantContextProvider tenantContextProvider() {
        return new DeterministicTenantContextProvider(TEST_TENANT);
    }

    @Bean
    OrganizationDirectory organizationDirectory() {
        return _ -> OrganizationAvailability.AVAILABLE;
    }

    @Bean
    UserDirectory userDirectory() {
        return (_, _) -> UserAvailability.AVAILABLE;
    }

    public static class DeterministicTenantContextProvider implements TenantContextProvider {

        private final AtomicInteger calls = new AtomicInteger();
        private volatile TenantContext tenantContext;

        public DeterministicTenantContextProvider(TenantContext tenantContext) {
            this.tenantContext = tenantContext;
        }

        @Override
        public TenantContext current() {
            calls.incrementAndGet();
            return tenantContext;
        }

        public void setTenantContext(TenantContext tenantContext) {
            this.tenantContext = tenantContext;
        }

        public int calls() {
            return calls.get();
        }

        public void resetCalls() {
            calls.set(0);
        }
    }
}
