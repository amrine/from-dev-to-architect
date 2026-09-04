package io.teampulse.identity.domain.user.model;

import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;

import java.util.EnumSet;
import java.util.Map;

public enum UserStatus {
    INVITED,
    CREATING,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED;

    private static final Map<UserStatus, EnumSet<UserStatus>> ALLOWED_TRANSITIONS =
        Map.of(
            INVITED, EnumSet.of(CREATING, DEACTIVATED),
            CREATING, EnumSet.of(ACTIVE, DEACTIVATED),
            ACTIVE, EnumSet.of(SUSPENDED, DEACTIVATED),
            SUSPENDED, EnumSet.of(ACTIVE, DEACTIVATED),
            DEACTIVATED, EnumSet.noneOf(UserStatus.class)
        );

    private boolean canTransitionTo(UserStatus target) {
        return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public void validateTransitionTo(UserStatus target) {
        if (!canTransitionTo(target)) {
            throw new UserException(
                UserErrorCode.INVALID_STATUS_TRANSITION,
                "Transition from %s to %s is not allowed"
                    .formatted(this, target)
            );
        }
    }
}
