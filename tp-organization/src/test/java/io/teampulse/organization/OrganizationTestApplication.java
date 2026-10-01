package io.teampulse.organization;

import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
@SpringBootApplication
public class OrganizationTestApplication {

    @Bean
    ReferenceFactory referenceFactory() {
        return new MonotonicReferenceFactory(Clock.systemUTC());
    }

    @Bean
    DeterministicUserDirectory userDirectory() {
        return new DeterministicUserDirectory();
    }

    public static class DeterministicUserDirectory implements UserDirectory {

        private final Map<UserKey, UserAvailability> availabilities =
            new ConcurrentHashMap<>();

        @Override
        public UserAvailability check(String organizationReference, String userReference) {
            return availabilities.getOrDefault(
                new UserKey(organizationReference, userReference),
                UserAvailability.NOT_FOUND
            );
        }

        public void setAvailability(
            String organizationReference,
            String userReference,
            UserAvailability availability
        ) {
            availabilities.put(
                new UserKey(organizationReference, userReference),
                availability
            );
        }

        public void reset() {
            availabilities.clear();
        }
    }

    private record UserKey(String organizationReference, String userReference) { }
}
