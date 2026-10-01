package io.teampulse.team.infrastructure.web.team;

public record TeamResponse(
    String reference,
    String name,
    String status,
    String administratorReference,
    String managerReference
) { }
