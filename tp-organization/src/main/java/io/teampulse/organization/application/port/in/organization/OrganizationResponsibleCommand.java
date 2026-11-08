package io.teampulse.organization.application.port.in.organization;

import jakarta.validation.constraints.NotBlank;

/**
 * Identifies the organization and user involved in a responsibility command.
 *
 * @param organizationReference functional reference of the target organization
 * @param responsibleReference functional user reference for the requested role
 */
public record OrganizationResponsibleCommand(
    @NotBlank String organizationReference,
    @NotBlank String responsibleReference
) { }
