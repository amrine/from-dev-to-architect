package io.teampulse.team;

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

@TestConfiguration
@SpringBootApplication
public class TeamTestApplication {

    @Bean
    Clock teamClock() {
        return Clock.systemUTC();
    }

    @Bean
    ReferenceFactory referenceFactory(Clock teamClock) {
        return new MonotonicReferenceFactory(teamClock);
    }

    @Bean
    OrganizationDirectory organizationDirectory() {
        return organizationReference -> OrganizationAvailability.AVAILABLE;
    }

    @Bean
    UserDirectory userDirectory() {
        return (organizationReference, userReference) -> UserAvailability.AVAILABLE;
    }
}
