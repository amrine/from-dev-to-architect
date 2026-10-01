package io.teampulse.organization.infrastructure.web.organization;

import jakarta.validation.constraints.NotBlank;

public record AssignOrganizationResponsibleUserRequest(
    @NotBlank String userReference
) { }
