package io.teampulse.organization.api.organization;

import java.util.Objects;

/**
 * Signals that the organization module could not check an organization's
 * availability.
 */
public class OrganizationDirectoryException extends RuntimeException {

    private static final String MESSAGE =
        "Unable to check organization availability";

    /**
     * Wraps a failed availability lookup while retaining its technical cause.
     * The exception message is independent of the underlying failure details.
     *
     * @param cause failure reported by the organization module
     * @throws NullPointerException if the cause is null
     */
    public OrganizationDirectoryException(Throwable cause) {
        super(MESSAGE, Objects.requireNonNull(cause, "cause must not be null"));
    }
}
