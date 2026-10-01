package io.teampulse.organization.api.organization;

import java.util.Objects;

/** Public result of organization provisioning. */
public record OrganizationProvisioningResult(
    String organizationReference,
    OrganizationLifecycleState status
) {

    public OrganizationProvisioningResult {
        Objects.requireNonNull(organizationReference, "organizationReference must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (organizationReference.isBlank()) {
            throw new IllegalArgumentException("organizationReference must not be blank");
        }
    }
}
