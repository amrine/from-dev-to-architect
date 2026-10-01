package io.teampulse.team.infrastructure.web.team;

import java.time.Instant;

public record TeamMemberResponse(
    String userReference,
    String status,
    Instant startedAt,
    Instant endedAt
) { }
