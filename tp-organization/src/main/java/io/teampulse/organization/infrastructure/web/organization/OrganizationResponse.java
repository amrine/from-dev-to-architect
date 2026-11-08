package io.teampulse.organization.infrastructure.web.organization;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * HTTP representation of an organization's business state.
 *
 * @param reference functional organization reference
 * @param name normalized organization name
 * @param timezone timezone identifier
 * @param status textual business status
 * @param administratorReference assigned administrator, or null if unassigned
 * @param managerReference assigned manager, or null if unassigned
 */
@Schema(name = "OrganizationResponse")
public record OrganizationResponse(
    String reference,
    String name,
    String timezone,
    @Schema(allowableValues = {"CREATING", "ACTIVE", "SUSPENDED", "ARCHIVED"})
    String status,
    String administratorReference,
    String managerReference
) { }
