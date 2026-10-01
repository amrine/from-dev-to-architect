package io.teampulse.organization.api.organization;

import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

/** Public query for organization lifecycle state and assigned responsibilities. */
public interface OrganizationResponsibilityDirectory {

    Optional<OrganizationResponsibilitySnapshot> findByOrganizationReference(
        @NotBlank String organizationReference
    );
}
