package io.teampulse.team.infrastructure.web.team;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TeamResponse")
public record TeamResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,

        @Schema(
                allowableValues = {"ACTIVE", "SUSPENDED", "ARCHIVED"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String status,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String administratorReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String managerReference) {}
