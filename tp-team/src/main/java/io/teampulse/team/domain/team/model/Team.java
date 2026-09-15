package io.teampulse.team.domain.team.model;

import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class Team {

    private static final int MAX_NAME_LENGTH = 200;
    private static final String TEAM_REFERENCE_PREFIX = "TEM";
    private static final String ORGANIZATION_REFERENCE_PREFIX = "ORG";
    private static final String USER_REFERENCE_PREFIX = "USR";

    private final Long id;
    private final String reference;
    private final String organizationReference;
    private final String name;
    private String adminReference;
    private String managerReference;
    private TeamStatus status;

    private Team(
            Long id,
            String reference,
            String organizationReference,
            String name,
            String adminReference,
            String managerReference,
            TeamStatus status) {
        this.id = id;
        this.reference = validateReference(reference, TEAM_REFERENCE_PREFIX, "reference");
        this.organizationReference =
                validateReference(organizationReference, ORGANIZATION_REFERENCE_PREFIX, "organizationReference");
        this.name = validateName(name);
        this.adminReference = validateUserReference(adminReference, "adminReference");
        this.managerReference = validateUserReference(managerReference, "managerReference");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * Creates an active team. The persistence identifier remains absent until
     * the team is stored and restored by the persistence adapter.
     */
    public static Team create(
            String reference,
            String organizationReference,
            String name,
            String adminReference,
            String managerReference) {
        return new Team(
                null, reference, organizationReference, name, adminReference, managerReference, TeamStatus.ACTIVE);
    }

    /**
     * Restores a persisted team after validating its business state.
     */
    public static Team restore(
            Long id,
            String reference,
            String organizationReference,
            String name,
            String adminReference,
            String managerReference,
            TeamStatus status) {
        return new Team(
                validateId(id), reference, organizationReference, name, adminReference, managerReference, status);
    }

    public void suspend() {
        transitionTo(TeamStatus.SUSPENDED);
    }

    public void reactivate() {
        transitionTo(TeamStatus.ACTIVE);
    }

    public void archive() {
        transitionTo(TeamStatus.ARCHIVED);
    }

    public void replaceAdministrator(String administratorReference) {
        validateReplacementAllowed("replaceAdministrator");
        String validatedReference = validateUserReference(administratorReference, "administratorReference");

        if (validatedReference.equals(adminReference)) {
            return;
        }

        adminReference = validatedReference;
    }

    public void replaceManager(String managerReference) {
        validateReplacementAllowed("replaceManager");
        String validatedReference = validateUserReference(managerReference, "managerReference");

        if (validatedReference.equals(this.managerReference)) {
            return;
        }

        this.managerReference = validatedReference;
    }

    private void transitionTo(TeamStatus target) {
        status.validateTransitionTo(target);
        status = target;
    }

    public void validateReplacementAllowed(String operation) {
        if (status != TeamStatus.ACTIVE && status != TeamStatus.SUSPENDED) {
            throw new TeamException(
                    TeamErrorCode.INVALID_STATUS_TRANSITION,
                    "Operation %s is not allowed from status %s".formatted(operation, status));
        }
    }

    private static Long validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        return id;
    }

    private static String validateUserReference(String reference, String fieldName) {
        return validateReference(reference, USER_REFERENCE_PREFIX, fieldName);
    }

    private static String validateReference(String reference, String expectedPrefix, String fieldName) {
        if (!ReferenceFormat.matches(reference, expectedPrefix)) {
            throw new IllegalArgumentException(fieldName + " does not match the expected reference format");
        }
        return reference;
    }

    private static String validateName(String name) {
        if (name == null) {
            throw invalidName();
        }

        String normalizedName = name.strip();
        if (normalizedName.isBlank() || normalizedName.length() > MAX_NAME_LENGTH) {
            throw invalidName();
        }
        return normalizedName;
    }

    private static TeamException invalidName() {
        return new TeamException(
                TeamErrorCode.INVALID_NAME,
                "name must not be null or blank and must not exceed " + MAX_NAME_LENGTH + " characters");
    }
}
