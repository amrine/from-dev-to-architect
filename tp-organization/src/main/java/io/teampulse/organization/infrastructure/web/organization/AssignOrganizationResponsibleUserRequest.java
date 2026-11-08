package io.teampulse.organization.infrastructure.web.organization;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * HTTP payload identifying a candidate for an organization responsibility.
 *
 * @param userReference required functional user reference
 */
@Schema(name = "ResponsibleUserInput")
public record AssignOrganizationResponsibleUserRequest(
    @NotBlank String userReference
) { }
