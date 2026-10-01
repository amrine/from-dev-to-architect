package io.teampulse.organization.api.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Synchronous public contract used by platform administration provisioning. */
public interface OrganizationProvisioning {

    OrganizationProvisioningResult createOrganization(
        @NotNull @Valid OrganizationProvisioningCommand command
    );
}
