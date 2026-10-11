package io.teampulse.organization.api.organization;

import java.util.Objects;

/** Public status and responsible-user references for platform orchestration. */
public record OrganizationResponsibilitySnapshot(
    String organizationReference,
    OrganizationLifecycleState status,
    String administratorReference,
    String managerReference
) {

    public OrganizationResponsibilitySnapshot {
        Objects.requireNonNull(organizationReference, "organizationReference must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (organizationReference.isBlank()) {
            throw new IllegalArgumentException("organizationReference must not be blank");
        }
    }
}
