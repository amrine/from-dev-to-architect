package io.teampulse.organization;

import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@TestConfiguration
@SpringBootApplication
public class OrganizationTestApplication {

    @Bean
    ReferenceFactory referenceFactory() {
        return new MonotonicReferenceFactory(Clock.systemUTC());
    }

    @Bean
    UserDirectory userDirectory() {
        return (_, _) -> UserAvailability.NOT_FOUND;
    }
}
