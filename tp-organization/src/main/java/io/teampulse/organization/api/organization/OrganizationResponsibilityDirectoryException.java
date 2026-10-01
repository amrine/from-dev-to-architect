package io.teampulse.organization.api.organization;

import java.util.Objects;

/** Signals that the public responsibility query failed for a technical reason. */
public class OrganizationResponsibilityDirectoryException extends RuntimeException {

    private static final String MESSAGE =
        "Unable to check organization responsibilities";

    public OrganizationResponsibilityDirectoryException(Throwable cause) {
        super(MESSAGE, Objects.requireNonNull(cause, "cause must not be null"));
    }
}
