package io.teampulse.identity.api.lifecycle;

import lombok.Getter;

import java.util.Objects;

/** Public identity failure without exposing identity's internal error types. */
@Getter
public class UserLifecycleException extends RuntimeException {

    private final UserLifecycleErrorCode errorCode;

    public UserLifecycleException(
        UserLifecycleErrorCode errorCode,
        Throwable cause
    ) {
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        super(Objects.requireNonNull(errorCode.getValue(), "message must not be null"), cause);
    }

}
