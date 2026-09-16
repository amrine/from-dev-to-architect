package io.teampulse.team.domain.team.model;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;

import java.util.EnumSet;
import java.util.Map;

public enum TeamStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED;

    private static final Map<TeamStatus, EnumSet<TeamStatus>> ALLOWED_TRANSITIONS = Map.of(
            ACTIVE, EnumSet.of(SUSPENDED, ARCHIVED),
            SUSPENDED, EnumSet.of(ACTIVE, ARCHIVED),
            ARCHIVED, EnumSet.noneOf(TeamStatus.class));

    private boolean canTransitionTo(TeamStatus target) {
        return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public void validateTransitionTo(TeamStatus target) {
        if (!canTransitionTo(target)) {
            throw new TeamException(
                    TeamErrorCode.INVALID_STATUS_TRANSITION,
                    "Transition from %s to %s is not allowed".formatted(this, target));
        }
    }
}
