package io.teampulse.team.infrastructure.web.team;

import jakarta.validation.constraints.NotBlank;

public record TeamMemberRequest(
    @NotBlank String userReference
) { }
