package io.teampulse.identity.api.lifecycle;

import jakarta.validation.constraints.NotBlank;

/** User data shared by the create and invite commands. The tenant is separate. */
public record UserProvisioningCommand(
    @NotBlank String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) { }
