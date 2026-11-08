package io.teampulse.organization.api.lifecycle;

import jakarta.validation.constraints.NotBlank;

/**
 * Public synchronous command for provisioning an organization.
 *
 * @param name business name to normalize and validate
 * @param timezone timezone identifier to validate
 */
public record OrganizationProvisioningCommand(
    @NotBlank String name,
    @NotBlank String timezone
) { }
