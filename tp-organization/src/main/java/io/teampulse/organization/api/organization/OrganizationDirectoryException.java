package io.teampulse.organization.api.organization;

import java.util.Objects;

/**
 * Signals that the organization module could not check an organization's
 * availability.
 */
public class OrganizationDirectoryException extends RuntimeException {

    private static final String MESSAGE =
        "Unable to check organization availability";

    public OrganizationDirectoryException(Throwable cause) {
        super(MESSAGE, Objects.requireNonNull(cause, "cause must not be null"));
    }
}
