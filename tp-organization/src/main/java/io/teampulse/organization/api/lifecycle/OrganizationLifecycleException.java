package io.teampulse.organization.api.lifecycle;

import lombok.Getter;

import java.util.Objects;

/** Public failure with a safe message and the original diagnostic cause. */
@Getter
public class OrganizationLifecycleException extends RuntimeException {

    private final OrganizationLifecycleErrorCode errorCode;

    /**
     * Creates a public failure with the code's safe message and original cause.
     *
     * @param errorCode public business or technical failure category
     * @param cause underlying diagnostic failure, or {@code null} if none is available
     * @throws NullPointerException if {@code errorCode} is null
     */
    public OrganizationLifecycleException(OrganizationLifecycleErrorCode errorCode, Throwable cause) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(), cause);
        this.errorCode = errorCode;
    }
}
