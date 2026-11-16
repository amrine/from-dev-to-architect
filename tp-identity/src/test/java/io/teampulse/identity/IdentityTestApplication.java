package io.teampulse.identity;

import io.teampulse.common.context.TenantContext;
import io.teampulse.testsupport.context.DeterministicTenantContextProviderTestConfiguration;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.reference.ReferenceFactoryTestConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@SpringBootApplication
@Import({
    PersistenceIntegrationTestConfiguration.class,
    DeterministicTenantContextProviderTestConfiguration.class,
    ReferenceFactoryTestConfiguration.class
})
public class IdentityTestApplication {

    private static final TenantContext TEST_TENANT = new TenantContext("ORG-2026-0908-00000ZA7B900");

    public static void main(String[] args) {
        SpringApplication.run(IdentityTestApplication.class, args);
    }

    @Bean
    TenantContext testTenantContext() {
        return TEST_TENANT;
    }
}
