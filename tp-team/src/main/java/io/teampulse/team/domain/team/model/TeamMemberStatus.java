package io.teampulse.team.domain.team.model;

import io.teampulse.team.domain.team.error.TeamMemberErrorCode;
import io.teampulse.team.domain.team.error.TeamMemberException;

import java.util.EnumSet;
import java.util.Map;

public enum TeamMemberStatus {
    INVITED,
    ACTIVE,
    SUSPENDED,
    REMOVED;

    private static final Map<TeamMemberStatus, EnumSet<TeamMemberStatus>> ALLOWED_TRANSITIONS = Map.of(
            INVITED, EnumSet.of(ACTIVE, REMOVED),
            ACTIVE, EnumSet.of(SUSPENDED, REMOVED),
            SUSPENDED, EnumSet.of(ACTIVE, REMOVED),
            REMOVED, EnumSet.noneOf(TeamMemberStatus.class));

    private boolean canTransitionTo(TeamMemberStatus target) {
        return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public void validateTransitionTo(TeamMemberStatus target) {
        if (!canTransitionTo(target)) {
            throw new TeamMemberException(
                    TeamMemberErrorCode.INVALID_STATUS_TRANSITION,
                    "Transition from %s to %s is not allowed".formatted(this, target));
        }
    }
}
