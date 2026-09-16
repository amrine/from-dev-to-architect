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
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
@SpringBootApplication
public class TeamTestApplication {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-16T08:30:00Z");

    @Bean
    Clock teamClock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }

    @Bean
    ReferenceFactory referenceFactory(Clock teamClock) {
        return new MonotonicReferenceFactory(teamClock);
    }

    @Bean
    OrganizationDirectory organizationDirectory() {
        return _ -> OrganizationAvailability.AVAILABLE;
    }

    @Bean
    UserDirectory userDirectory() {
        return (_, _) -> UserAvailability.AVAILABLE;
    }
}
