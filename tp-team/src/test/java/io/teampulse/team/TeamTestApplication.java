package io.teampulse.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilities;
import io.teampulse.testsupport.context.DeterministicTenantContextProviderTestConfiguration;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.reference.ReferenceFactoryTestConfiguration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Optional;

@TestConfiguration
@SpringBootApplication
@Import({
    PersistenceIntegrationTestConfiguration.class,
    DeterministicTenantContextProviderTestConfiguration.class,
    ReferenceFactoryTestConfiguration.class
})
public class TeamTestApplication {

    private static final TenantContext TEST_TENANT = new TenantContext("ORG-2026-0916-00000ZA7B950");

    public static void main(String[] args) {
        SpringApplication.run(TeamTestApplication.class, args);
    }

    @Bean
    TenantContext testTenantContext() {
        return TEST_TENANT;
    }

    @Bean
    OrganizationDirectory organizationDirectory() {
        return new OrganizationDirectory() {
            @Override
            public OrganizationAvailability check(String organizationReference) {
                return OrganizationAvailability.AVAILABLE;
            }

            @Override
            public Optional<OrganizationResponsibilities> findByOrganizationReference(String organizationReference) {
                return Optional.empty();
            }
        };
    }

    @Bean
    UserDirectory userDirectory() {
        return (_, _) -> UserAvailability.AVAILABLE;
    }
}
