package io.teampulse.team.infrastructure.web.teammember;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "TeamMemberResponse")
public record TeamMemberResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String userReference,

        @Schema(
                allowableValues = {"INVITED", "ACTIVE", "SUSPENDED", "REMOVED"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String status,

        @Schema(format = "date-time", nullable = true) Instant startedAt,
        @Schema(format = "date-time", nullable = true) Instant endedAt) {}
