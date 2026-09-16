package io.teampulse.team.application.port.in.team;

import jakarta.validation.constraints.NotBlank;

public record TeamResponsibleCommand(
        @NotBlank String teamReference, @NotBlank String responsibleReference) {}
