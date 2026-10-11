package io.teampulse.organization.infrastructure.web.organization;

public record OrganizationResponse(
    String reference,
    String name,
    String timezone,
    String status,
    String administratorReference,
    String managerReference
) { }
