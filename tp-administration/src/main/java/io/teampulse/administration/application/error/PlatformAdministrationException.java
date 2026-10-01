package io.teampulse.administration.application.error;

import java.util.Objects;

public class PlatformAdministrationException extends RuntimeException {

    private final PlatformAdministrationErrorCode errorCode;

    public PlatformAdministrationException(
        PlatformAdministrationErrorCode errorCode,
        String message
    ) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public PlatformAdministrationException(
        PlatformAdministrationErrorCode errorCode,
        String message,
        Throwable cause
    ) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public PlatformAdministrationErrorCode getErrorCode() {
        return errorCode;
    }
}
