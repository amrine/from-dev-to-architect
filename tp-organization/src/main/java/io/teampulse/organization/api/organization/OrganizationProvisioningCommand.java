package io.teampulse.organization.api.organization;

import jakarta.validation.constraints.NotBlank;

/** Public synchronous command for provisioning an organization. */
public record OrganizationProvisioningCommand(
    @NotBlank String name,
    @NotBlank String timezone
) { }
