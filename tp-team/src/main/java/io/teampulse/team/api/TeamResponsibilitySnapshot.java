package io.teampulse.team.api;

import java.util.Objects;

/** Team reference and status returned for a user's administrator or manager responsibility. */
public record TeamResponsibilitySnapshot(
    String teamReference,
    TeamLifecycleState status
) {

    public TeamResponsibilitySnapshot {
        Objects.requireNonNull(teamReference, "teamReference must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (teamReference.isBlank()) {
            throw new IllegalArgumentException("teamReference must not be blank");
        }
    }
}
