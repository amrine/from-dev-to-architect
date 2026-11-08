package io.teampulse.organization.fixture;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Supplies configured user availabilities, defaulting to not found within each organization. */
public class DeterministicUserDirectory implements UserDirectory {

    private final Map<UserKey, UserAvailability> availabilities = new ConcurrentHashMap<>();

    /** Returns the configured availability, or not found when no user was configured. */
    @Override
    public UserAvailability check(String organizationReference, String userReference) {
        return availabilities.getOrDefault(
            new UserKey(organizationReference, userReference),
            UserAvailability.NOT_FOUND
        );
    }

    /** Configures a user's availability within the specified organization. */
    public void setAvailability(
        String organizationReference,
        String userReference,
        UserAvailability availability
    ) {
        availabilities.put(new UserKey(organizationReference, userReference), availability);
    }

    /** Clears configured users before the next scenario. */
    public void reset() {
        availabilities.clear();
    }

    private record UserKey(String organizationReference, String userReference) { }
}
