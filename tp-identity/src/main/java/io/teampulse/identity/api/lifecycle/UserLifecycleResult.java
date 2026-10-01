package io.teampulse.identity.api.lifecycle;

import java.util.Objects;

/** Public result of a user lifecycle command. */
public record UserLifecycleResult(String userReference) {

    public UserLifecycleResult {
        Objects.requireNonNull(userReference, "userReference must not be null");
        if (userReference.isBlank()) {
            throw new IllegalArgumentException("userReference must not be blank");
        }
    }
}
