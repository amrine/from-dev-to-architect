package io.teampulse.administration.application.port.in;

import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Shared provisioning data; the operation method states whether the user is created or invited. */
public record PlatformProvisioningCommand(
    @NotNull @Valid OrganizationProvisioningCommand organization,
    @NotNull @Valid UserProvisioningCommand initialUser
) { }
