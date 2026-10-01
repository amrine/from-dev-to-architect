package io.teampulse.identity.api.lifecycle;

import java.util.Objects;

/** Public identity failure without exposing identity's internal error types. */
public class UserLifecycleException extends RuntimeException {

    private final UserLifecycleErrorCode errorCode;

    public UserLifecycleException(
        UserLifecycleErrorCode errorCode,
        String message
    ) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public UserLifecycleErrorCode getErrorCode() {
        return errorCode;
    }
}
