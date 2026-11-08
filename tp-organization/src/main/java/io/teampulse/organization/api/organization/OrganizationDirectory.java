package io.teampulse.organization.api.organization;

import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

/**
 * Public read-only contract for organization availability and assigned responsibilities.
 */
public interface OrganizationDirectory {

    /**
     * Checks the availability derived from the persisted organization status.
     *
     * @param organizationReference organization to check
     * @return the organization's public availability
     * @throws OrganizationDirectoryException when the organization cannot be
     *         checked because of a technical failure
     */
    OrganizationAvailability check(
        @NotBlank String organizationReference
    );

    /**
     * Reads the current status and responsible-user references of an organization.
     * The result exposes no persistence entity or domain model.
     *
     * @param organizationReference functional reference of the organization
     * @return the organization responsibilities, or empty if it does not exist
     * @throws OrganizationDirectoryException if the query fails technically
     */
    Optional<OrganizationResponsibilities> findByOrganizationReference(
        @NotBlank String organizationReference
    );

}
