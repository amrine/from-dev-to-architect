package io.teampulse.organization.domain.organization.model;

import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;

import java.util.EnumSet;
import java.util.Map;

public enum OrganizationStatus {
    CREATING,
    ACTIVE,
    SUSPENDED,
    ARCHIVED;

    private static final Map<OrganizationStatus, EnumSet<OrganizationStatus>> ALLOWED_TRANSITIONS =
        Map.of(
            CREATING, EnumSet.of(ACTIVE, ARCHIVED),
            ACTIVE, EnumSet.of(SUSPENDED, ARCHIVED),
            SUSPENDED, EnumSet.of(ACTIVE, ARCHIVED),
            ARCHIVED, EnumSet.noneOf(OrganizationStatus.class)
        );

    private boolean canTransitionTo(OrganizationStatus target) {
        return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public void validateTransitionTo(OrganizationStatus target) {
        if (!canTransitionTo(target)) {
            throw new OrganizationException(
                OrganizationErrorCode.INVALID_STATUS_TRANSITION,
                "Transition from %s to %s is not allowed"
                    .formatted(this, target)
            );
        }
    }
}
