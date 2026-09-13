package io.teampulse.organization.application.port.in.organization;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationCommand(
    @NotBlank String name,
    @NotBlank String timezone
) { }
