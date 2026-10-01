package io.teampulse.team.infrastructure.web.team;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
    @NotBlank String name,
    @NotBlank String administratorReference,
    @NotBlank String managerReference
) { }
