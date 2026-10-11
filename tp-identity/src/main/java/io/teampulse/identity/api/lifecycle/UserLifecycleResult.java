package io.teampulse.identity.api.lifecycle;

import java.util.Objects;

/** Public snapshot returned after creating or inviting a user. */
public record UserLifecycleResult(
    String userReference,
    String email,
    String firstName,
    String lastName,
    String status
) {

    public UserLifecycleResult {
        Objects.requireNonNull(userReference, "userReference must not be null");
        if (userReference.isBlank()) {
            throw new IllegalArgumentException("userReference must not be blank");
        }
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(firstName, "firstName must not be null");
        Objects.requireNonNull(lastName, "lastName must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
