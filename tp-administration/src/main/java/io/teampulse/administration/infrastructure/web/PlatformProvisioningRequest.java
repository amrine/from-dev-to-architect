package io.teampulse.administration.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record PlatformProvisioningRequest(
    @NotBlank String organizationName,
    @NotBlank String timezone,
    @NotBlank String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) { }
