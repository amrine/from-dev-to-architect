package io.teampulse.team.infrastructure.web.team;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ResponsibleUserInput")
public record TeamResponsibleUserRequest(
        @Schema(minLength = 1) @NotBlank String userReference) {}
