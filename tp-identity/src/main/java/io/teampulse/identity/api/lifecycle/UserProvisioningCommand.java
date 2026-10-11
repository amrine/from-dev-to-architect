package io.teampulse.identity.api.lifecycle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Public input for creating or inviting a user. */
public record UserProvisioningCommand(
    @Schema(format = "email", maxLength = 254, pattern = "^[^\\s@]+@[^\\s@]+$")
    @NotBlank String email,
    @Schema(minLength = 1, maxLength = 100)
    @NotBlank String firstName,
    @Schema(minLength = 1, maxLength = 100)
    @NotBlank String lastName
) { }
