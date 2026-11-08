package io.teampulse.organization.domain.organization.error;

import lombok.Getter;

import java.util.Objects;

public class OrganizationException extends RuntimeException {

    @Getter
    private final OrganizationErrorCode errorCode;

    /**
     * Creates a domain failure without an underlying cause.
     *
     * @param errorCode organization-specific failure category
     * @param message diagnostic message
     * @throws NullPointerException if the code or message is null
     */
    public OrganizationException(OrganizationErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * Creates a domain failure while retaining an optional diagnostic cause.
     *
     * @param errorCode organization-specific failure category
     * @param message diagnostic message
     * @param cause underlying failure, or null when none applies
     * @throws NullPointerException if the code or message is null
     */
    public OrganizationException(
        OrganizationErrorCode errorCode,
        String message,
        Throwable cause
    ) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
