package io.teampulse.identity.domain.user.error;

import lombok.Getter;

import java.util.Objects;

public class UserException extends RuntimeException {

    @Getter
    private final UserErrorCode errorCode;

    public UserException(UserErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public UserException(
        UserErrorCode errorCode,
        String message,
        Throwable cause
    ) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
