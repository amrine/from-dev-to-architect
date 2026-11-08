package io.teampulse.organization.api.organization;

import java.util.Objects;

/**
 * Public organization status and assigned responsibilities shared by the Java and HTTP APIs.
 *
 * @param organizationReference functional organization reference
 * @param status textual business status
 * @param administratorReference assigned administrator, or null if unassigned
 * @param managerReference assigned manager, or null if unassigned
 */
public record OrganizationResponsibilities(
    String organizationReference,
    String status,
    String administratorReference,
    String managerReference
) {

    /**
     * Validates the required organization reference and status.
     * Responsible-user references remain nullable when no user occupies the role.
     *
     * @throws NullPointerException if the organization reference or status is null
     * @throws IllegalArgumentException if the organization reference is blank
     */
    public OrganizationResponsibilities {
        Objects.requireNonNull(organizationReference, "organizationReference must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (organizationReference.isBlank()) {
            throw new IllegalArgumentException("organizationReference must not be blank");
        }
    }
}
