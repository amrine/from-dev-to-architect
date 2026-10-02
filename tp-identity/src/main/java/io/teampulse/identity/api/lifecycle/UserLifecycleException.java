package io.teampulse.identity.api.lifecycle;

import java.util.Objects;

/** Public identity failure without exposing identity's internal error types. */
public class UserLifecycleException extends RuntimeException {

    private final UserLifecycleErrorCode errorCode;

    public UserLifecycleException(
        UserLifecycleErrorCode errorCode,
        String message
    ) {
        this(errorCode, message, null);
    }

    public UserLifecycleException(
        UserLifecycleErrorCode errorCode,
        String message,
        Throwable cause
    ) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public UserLifecycleErrorCode getErrorCode() {
        return errorCode;
    }
}
