package io.teampulse.administration.application.error;

import lombok.Getter;

import java.util.Objects;

@Getter
public class PlatformAdministrationException extends RuntimeException {

    private final PlatformAdministrationErrorCode errorCode;

    public PlatformAdministrationException(
        PlatformAdministrationErrorCode errorCode
    ) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = errorCode;
    }

    public PlatformAdministrationException(
        PlatformAdministrationErrorCode errorCode,
        Throwable cause
    ) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(), cause);
        this.errorCode = errorCode;
    }

}
