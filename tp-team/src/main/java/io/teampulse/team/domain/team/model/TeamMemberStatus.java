package io.teampulse.team.domain.team.model;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;

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
            throw new TeamException(
                    TeamErrorCode.INVALID_MEMBER_STATUS_TRANSITION,
                    "Transition from %s to %s is not allowed".formatted(this, target));
        }
    }
}
