package io.teampulse.team;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

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
        return _ -> OrganizationAvailability.NOT_FOUND;
    }
}
