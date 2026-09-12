package io.teampulse.organization.domain.organization.error;

import lombok.Getter;

import java.util.Objects;

public class OrganizationException extends RuntimeException {

    @Getter
    private final OrganizationErrorCode errorCode;

    public OrganizationException(OrganizationErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public OrganizationException(
        OrganizationErrorCode errorCode,
        String message,
        Throwable cause
    ) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
