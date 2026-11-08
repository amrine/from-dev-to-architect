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

    /**
     * Checks the lifecycle transition table without changing state.
     *
     * @param target proposed target status, possibly null
     * @return true only for a non-null target allowed from this status
     */
    private boolean canTransitionTo(OrganizationStatus target) {
        return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /**
     * Requires a transition allowed by the lifecycle table.
     * Responsible-user invariants are checked separately by the organization model.
     *
     * @param target proposed target status
     * @throws OrganizationException with {@code INVALID_STATUS_TRANSITION} if
     *         the transition is forbidden, including a null target
     */
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
