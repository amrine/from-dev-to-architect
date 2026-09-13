package io.teampulse.organization.application.port.in.organization;

import jakarta.validation.constraints.NotBlank;

public record OrganizationResponsibleCommand(
    @NotBlank String organizationReference,
    @NotBlank String responsibleReference
) { }
