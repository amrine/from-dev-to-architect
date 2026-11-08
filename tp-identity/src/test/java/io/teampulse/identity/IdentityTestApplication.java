package io.teampulse.identity;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.fixture.DeterministicTenantContextProvider;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;

@TestConfiguration
@SpringBootApplication
@Import(PersistenceIntegrationTestConfiguration.class)
public class IdentityTestApplication {

    private static final TenantContext TEST_TENANT = new TenantContext("ORG-2026-0908-00000ZA7B900");

    public static void main(String[] args) {
        SpringApplication.run(IdentityTestApplication.class, args);
    }

    @Bean
    ReferenceFactory referenceFactory() {
        return new MonotonicReferenceFactory(Clock.systemUTC());
    }

    @Bean
    DeterministicTenantContextProvider testTenantContextProvider() {
        return new DeterministicTenantContextProvider(TEST_TENANT);
    }
}
