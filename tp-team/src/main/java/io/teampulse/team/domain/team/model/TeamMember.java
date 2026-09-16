package io.teampulse.team.domain.team.model;

import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public final class TeamMember {

    private static final String ORGANIZATION_REFERENCE_PREFIX = "ORG";
    private static final String USER_REFERENCE_PREFIX = "USR";

    private final Long id;
    private final String organizationReference;
    private final Long teamId;
    private final String userReference;
    private TeamMemberStatus status;
    private Instant startedAt;
    private Instant endedAt;

    private TeamMember(
            Long id,
            String organizationReference,
            Long teamId,
            String userReference,
            TeamMemberStatus status,
            Instant startedAt,
            Instant endedAt) {
        this.id = id;
        this.organizationReference =
                validateReference(organizationReference, ORGANIZATION_REFERENCE_PREFIX, "organizationReference");
        this.teamId = validateId(teamId, "teamId");
        this.userReference = validateReference(userReference, USER_REFERENCE_PREFIX, "userReference");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        validateTemporalState();
    }

    /**
     * Creates a pending invitation without an effective membership period.
     */
    public static TeamMember invite(String organizationReference, Long teamId, String userReference) {
        return new TeamMember(null, organizationReference, teamId, userReference, TeamMemberStatus.INVITED, null, null);
    }

    /**
     * Creates an effective membership with its initial start time.
     */
    public static TeamMember add(String organizationReference, Long teamId, String userReference, Instant startedAt) {
        return new TeamMember(
                null, organizationReference, teamId, userReference, TeamMemberStatus.ACTIVE, startedAt, null);
    }

    /**
     * Restores a persisted membership after validating its temporal state.
     */
    public static TeamMember restore(
            Long id,
            String organizationReference,
            Long teamId,
            String userReference,
            TeamMemberStatus status,
            Instant startedAt,
            Instant endedAt) {
        return new TeamMember(
                validateId(id, "id"), organizationReference, teamId, userReference, status, startedAt, endedAt);
    }

    public void activate(Instant startedAt) {
        validateOperationAllowed(status == TeamMemberStatus.INVITED, "activate");
        this.startedAt = requireInstant(startedAt, "startedAt");
        status = TeamMemberStatus.ACTIVE;
    }

    public void suspend() {
        transitionTo(TeamMemberStatus.SUSPENDED);
    }

    public void reactivate() {
        transitionTo(TeamMemberStatus.ACTIVE);
    }

    public void remove(Instant endedAt) {
        status.validateTransitionTo(TeamMemberStatus.REMOVED);
        Instant validatedEndedAt = requireInstant(endedAt, "endedAt");
        validateEndedAt(validatedEndedAt);

        this.endedAt = validatedEndedAt;
        status = TeamMemberStatus.REMOVED;
    }

    private void transitionTo(TeamMemberStatus target) {
        status.validateTransitionTo(target);
        status = target;
    }

    private void validateOperationAllowed(boolean allowed, String operation) {
        if (!allowed) {
            throw new TeamException(
                    TeamErrorCode.INVALID_MEMBER_STATUS_TRANSITION,
                    "Operation %s is not allowed from status %s".formatted(operation, status));
        }
    }

    private void validateTemporalState() {
        boolean invalidState =
                switch (status) {
                    case INVITED -> startedAt != null || endedAt != null;
                    case ACTIVE, SUSPENDED -> startedAt == null || endedAt != null;
                    case REMOVED -> endedAt == null;
                };

        if (invalidState) {
            throw new IllegalArgumentException("status %s does not match membership dates".formatted(status));
        }

        if (endedAt != null) {
            validateEndedAt(endedAt);
        }
    }

    private void validateEndedAt(Instant endedAt) {
        if (startedAt != null && endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must not be before startedAt");
        }
    }

    private static Long validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return id;
    }

    private static String validateReference(String reference, String expectedPrefix, String fieldName) {
        if (!ReferenceFormat.matches(reference, expectedPrefix)) {
            throw new IllegalArgumentException(fieldName + " does not match the expected reference format");
        }
        return reference;
    }

    private static Instant requireInstant(Instant value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
