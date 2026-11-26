package io.teampulse.administration.application.port.in.identity;

import java.util.Objects;

/** Organization and user references after creating or inviting an initial user. */
public record UserLifecycleAdministrationResult(
    String organizationReference,
    String userReference
) {

    public UserLifecycleAdministrationResult {
        Objects.requireNonNull(organizationReference, "organizationReference must not be null");
        Objects.requireNonNull(userReference, "userReference must not be null");
        if (organizationReference.isBlank() || userReference.isBlank()) {
            throw new IllegalArgumentException("provisioned references must not be blank");
        }
    }
}
