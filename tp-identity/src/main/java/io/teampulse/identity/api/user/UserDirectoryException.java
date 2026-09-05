package io.teampulse.identity.api.user;

import java.util.Objects;

/**
 * Signals that the identity module could not check a user's availability.
 */
public class UserDirectoryException extends RuntimeException {

    private static final String MESSAGE = "Unable to check user availability";

    public UserDirectoryException(Throwable cause) {
        super(MESSAGE, Objects.requireNonNull(cause, "cause must not be null"));
    }
}
