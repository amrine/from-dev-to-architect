package io.teampulse.organization.infrastructure.web.organization;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(
    @NotBlank String name,
    @NotBlank String timezone
) { }
