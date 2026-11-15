package io.teampulse.team.infrastructure.web.teammember;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "TeamMemberInput")
public record TeamMemberRequest(
        @Schema(minLength = 1) @NotBlank String userReference) {}
