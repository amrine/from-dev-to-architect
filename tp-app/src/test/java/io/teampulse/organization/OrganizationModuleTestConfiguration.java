package io.teampulse.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Supplies the public identity contract while the organization module test is
 * bootstrapped in standalone mode.
 */
@TestConfiguration
class OrganizationModuleTestConfiguration {

    @Bean
    UserDirectory userDirectory() {
        return (_, _) -> UserAvailability.NOT_FOUND;
    }
}
