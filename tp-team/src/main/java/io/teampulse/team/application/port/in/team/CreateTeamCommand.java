package io.teampulse.team.application.port.in.team;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamCommand(
        @NotBlank String name,
        @NotBlank String adminReference,
        @NotBlank String managerReference) {}
