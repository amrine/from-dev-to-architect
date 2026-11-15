package io.teampulse.team.application.port.in.teammember;

import jakarta.validation.constraints.NotBlank;

public record TeamMemberCommand(
        @NotBlank String teamReference, @NotBlank String userReference) {}
