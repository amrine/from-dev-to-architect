package io.teampulse.organization.api.organization;

import jakarta.validation.constraints.NotBlank;

/**
 * Public organization contract for checking whether an organization is
 * operational.
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
}
