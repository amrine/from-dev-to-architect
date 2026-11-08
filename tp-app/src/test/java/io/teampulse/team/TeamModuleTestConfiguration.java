package io.teampulse.team;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilities;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * Supplies public dependencies while the team module is bootstrapped in
 * standalone mode.
 */
@TestConfiguration
class TeamModuleTestConfiguration {

    @Bean
    UserDirectory userDirectory() {
        return (_, _) -> UserAvailability.NOT_FOUND;
    }

    @Bean
    OrganizationDirectory organizationDirectory() {
        return new OrganizationDirectory() {
            @Override
            public OrganizationAvailability check(String organizationReference) {
                return OrganizationAvailability.NOT_FOUND;
            }

            @Override
            public Optional<OrganizationResponsibilities> findByOrganizationReference(String organizationReference) {
                return Optional.empty();
            }
        };
    }
}
