package io.teampulse.team.infrastructure.web.team;

import jakarta.validation.constraints.NotBlank;

public record TeamResponsibleUserRequest(
    @NotBlank String userReference
) { }
