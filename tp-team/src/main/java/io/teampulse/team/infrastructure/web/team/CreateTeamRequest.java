package io.teampulse.team.infrastructure.web.team;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateTeamInput")
public record CreateTeamRequest(
        @Schema(minLength = 1, maxLength = 200) @NotBlank String name,
        @Schema(minLength = 1) @NotBlank String administratorReference,
        @Schema(minLength = 1) @NotBlank String managerReference) {}
