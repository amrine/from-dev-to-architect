package io.teampulse.organization.domain.organization.model;

import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import lombok.Getter;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Objects;

@Getter
public final class Organization {

    private static final int MAX_NAME_LENGTH = 200;
    private static final String ORGANIZATION_REFERENCE_PREFIX = "ORG";
    private static final String USER_REFERENCE_PREFIX = "USR";

    private final String reference;
    private final String name;
    private final ZoneId timezone;
    private String adminReference;
    private String managerReference;
    private OrganizationStatus status;

    /**
     * Builds a valid domain state for creation or restoration.
     * References are checked without normalization; the name is stripped and the
     * status-specific responsible-user structure is validated.
     *
     * @param reference functional organization reference
     * @param name business name to normalize
     * @param timezone timezone identifier to resolve
     * @param adminReference optional administrator reference
     * @param managerReference optional manager reference
     * @param status initial or restored business status
     * @throws OrganizationException for an invalid name, timezone or responsible-user structure
     * @throws IllegalArgumentException if a supplied reference has an invalid format
     * @throws NullPointerException if the status is null
     */
    private Organization(
        String reference,
        String name,
        String timezone,
        String adminReference,
        String managerReference,
        OrganizationStatus status
    ) {
        this.reference = validateReference(
            reference,
            ORGANIZATION_REFERENCE_PREFIX,
            "reference"
        );
        this.name = validateName(name);
        this.timezone = validateTimezone(timezone);
        this.adminReference = validateOptionalUserReference(
            adminReference,
            "adminReference"
        );
        this.managerReference = validateOptionalUserReference(
            managerReference,
            "managerReference"
        );
        this.status = Objects.requireNonNull(status, "status must not be null");
        validateResponsibleStructure(status);
    }

    /**
     * Creates an organization in {@link OrganizationStatus#CREATING} without
     * responsible users. Technical persistence identity, version and audit
     * data remain outside this domain model.
     *
     * @param reference functional organization reference
     * @param name organization name, normalized with {@link String#strip()}
     * @param timezone timezone identifier recognized by {@link ZoneId}
     * @return a new organization in {@code CREATING} status
     */
    public static Organization create(
        String reference,
        String name,
        String timezone
    ) {
        return new Organization(
            reference,
            name,
            timezone,
            null,
            null,
            OrganizationStatus.CREATING
        );
    }

    /**
     * Reconstitutes the business state of a previously persisted organization
     * without applying the creation workflow. Technical persistence state
     * remains on the persistence entity.
     *
     * @param reference functional organization reference
     * @param name organization name, normalized with {@link String#strip()}
     * @param timezone timezone identifier recognized by {@link ZoneId}
     * @param adminReference optional administrator reference
     * @param managerReference optional manager reference
     * @param status persisted business status
     * @return the restored organization after structural invariant validation
     */
    public static Organization restore(
        String reference,
        String name,
        String timezone,
        String adminReference,
        String managerReference,
        OrganizationStatus status
    ) {
        return new Organization(
            reference,
            name,
            timezone,
            adminReference,
            managerReference,
            status
        );
    }

    /**
     * Assigns or replaces the candidate administrator while the organization
     * is being created. Assigning the current reference is a no-op and does not
     * activate the organization.
     *
     * @param administratorReference valid functional user reference
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         organization is not in {@code CREATING} status
     */
    public void assignAdministrator(String administratorReference) {
        validateOperationAllowed(
            status == OrganizationStatus.CREATING,
            "assignAdministrator"
        );
        String validatedReference = validateUserReference(
            administratorReference,
            "administratorReference"
        );

        if (validatedReference.equals(adminReference)) {
            return;
        }

        adminReference = validatedReference;
    }

    /**
     * Assigns or replaces the candidate manager while the organization is
     * being created, or assigns a missing manager to a suspended organization.
     * The operation does not activate or reactivate the organization.
     *
     * @param managerReference valid functional user reference
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         current status or manager state does not allow the assignment
     */
    public void assignManager(String managerReference) {
        boolean canAssign = status == OrganizationStatus.CREATING
            || (
                status == OrganizationStatus.SUSPENDED
                    && this.managerReference == null
            );
        validateOperationAllowed(canAssign, "assignManager");
        String validatedReference = validateUserReference(
            managerReference,
            "managerReference"
        );

        if (validatedReference.equals(this.managerReference)) {
            return;
        }

        this.managerReference = validatedReference;
    }

    /**
     * Activates an organization being created when both responsible user
     * references are present. Their availability must be checked beforehand by
     * the application layer.
     *
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         organization is not in {@code CREATING} status, or with
     *         {@link OrganizationErrorCode#ACTIVATION_REQUIREMENTS_NOT_MET}
     *         when a responsible user reference is missing
     */
    public void activate() {
        validateOperationAllowed(
            status == OrganizationStatus.CREATING,
            "activate"
        );
        transitionTo(OrganizationStatus.ACTIVE);
    }

    /**
     * Atomically replaces the administrator of an active or suspended
     * organization without changing its status. Reusing the current reference
     * is a no-op. User availability must be checked by the application layer.
     *
     * @param administratorReference valid functional user reference
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         current status does not allow replacement
     */
    public void replaceAdministrator(String administratorReference) {
        validateOperationAllowed(
            status == OrganizationStatus.ACTIVE
                || status == OrganizationStatus.SUSPENDED,
            "replaceAdministrator"
        );
        String validatedReference = validateUserReference(
            administratorReference,
            "administratorReference"
        );

        if (validatedReference.equals(adminReference)) {
            return;
        }

        adminReference = validatedReference;
    }

    /**
     * Atomically replaces an existing manager of an active or suspended
     * organization without changing its status. Reusing the current reference
     * is a no-op. User availability must be checked by the application layer.
     *
     * @param managerReference valid functional user reference
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         current status does not allow replacement or no manager exists
     */
    public void replaceManager(String managerReference) {
        boolean canReplace = (
            status == OrganizationStatus.ACTIVE
                || status == OrganizationStatus.SUSPENDED
        ) && this.managerReference != null;
        validateOperationAllowed(canReplace, "replaceManager");
        String validatedReference = validateUserReference(
            managerReference,
            "managerReference"
        );

        if (validatedReference.equals(this.managerReference)) {
            return;
        }

        this.managerReference = validatedReference;
    }

    /**
     * Removes the manager from an active or suspended organization. An active
     * organization is suspended before the reference is removed. Removing an
     * already absent manager from a suspended organization is a no-op.
     *
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         current status does not allow removal
     */
    public void removeManager() {
        validateOperationAllowed(
            status == OrganizationStatus.ACTIVE
                || status == OrganizationStatus.SUSPENDED,
            "removeManager"
        );

        if (status == OrganizationStatus.ACTIVE) {
            transitionTo(OrganizationStatus.SUSPENDED);
        }

        managerReference = null;
    }

    /**
     * Suspends an active organization while preserving both responsible user
     * references.
     *
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         organization is not active
     */
    public void suspend() {
        validateOperationAllowed(
            status == OrganizationStatus.ACTIVE,
            "suspend"
        );
        transitionTo(OrganizationStatus.SUSPENDED);
    }

    /**
     * Reactivates a suspended organization when both responsible user
     * references are present. Their availability must be checked beforehand by
     * the application layer.
     *
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         organization is not suspended, or with
     *         {@link OrganizationErrorCode#ACTIVATION_REQUIREMENTS_NOT_MET}
     *         when a responsible user reference is missing
     */
    public void reactivate() {
        validateOperationAllowed(
            status == OrganizationStatus.SUSPENDED,
            "reactivate"
        );
        transitionTo(OrganizationStatus.ACTIVE);
    }

    /**
     * Archives a creating, active or suspended organization while preserving
     * any responsible user references already present. Archived organizations
     * are terminal and cannot be archived again.
     *
     * @throws OrganizationException with
     *         {@link OrganizationErrorCode#INVALID_STATUS_TRANSITION} when the
     *         organization is already archived
     */
    public void archive() {
        transitionTo(OrganizationStatus.ARCHIVED);
    }

    /**
     * Validates the lifecycle transition and the target structural invariants
     * before mutating the current status.
     */
    private void transitionTo(OrganizationStatus target) {
        status.validateTransitionTo(target);
        validateResponsibleStructure(target);
        status = target;
    }

    /**
     * Checks the responsible references required by a proposed business status.
     * Active organizations need both roles; suspended organizations need an administrator.
     * This local invariant does not establish current user availability.
     *
     * @param statusToValidate status whose structural requirements are evaluated
     * @throws OrganizationException if the required responsible references are missing
     */
    private void validateResponsibleStructure(OrganizationStatus statusToValidate) {
        boolean requirementsNotMet = switch (statusToValidate) {
            case ACTIVE -> adminReference == null || managerReference == null;
            case SUSPENDED -> adminReference == null;
            case CREATING, ARCHIVED -> false;
        };

        if (requirementsNotMet) {
            throw new OrganizationException(
                OrganizationErrorCode.ACTIVATION_REQUIREMENTS_NOT_MET,
                "Organization status %s does not meet responsible user requirements"
                    .formatted(statusToValidate)
            );
        }
    }

    /**
     * Rejects an operation whose state-specific precondition is false.
     *
     * @param allowed whether the operation is permitted in the current state
     * @param operation business operation name used in the diagnostic message
     * @throws OrganizationException with {@code INVALID_STATUS_TRANSITION} if disallowed
     */
    private void validateOperationAllowed(boolean allowed, String operation) {
        if (!allowed) {
            throw new OrganizationException(
                OrganizationErrorCode.INVALID_STATUS_TRANSITION,
                "Operation %s is not allowed from status %s"
                    .formatted(operation, status)
            );
        }
    }

    /**
     * Validates a responsible-user reference only when one is present.
     *
     * @param reference optional functional user reference
     * @param fieldName field name used in diagnostics
     * @return the unchanged reference, or null when the role is unassigned
     * @throws IllegalArgumentException if a non-null reference has an invalid format
     */
    private static String validateOptionalUserReference(
        String reference,
        String fieldName
    ) {
        if (reference == null) {
            return null;
        }
        return validateUserReference(reference, fieldName);
    }

    /**
     * Requires an unchanged functional reference with the user prefix.
     *
     * @param reference required functional user reference
     * @param fieldName field name used in diagnostics
     * @return the validated reference
     * @throws IllegalArgumentException if the reference is null or has an invalid format
     */
    private static String validateUserReference(
        String reference,
        String fieldName
    ) {
        return validateReference(reference, USER_REFERENCE_PREFIX, fieldName);
    }

    /**
     * Checks the shared functional-reference format and the expected business prefix.
     *
     * @param reference value to inspect without normalization
     * @param expectedPrefix required entity prefix
     * @param fieldName field name used in diagnostics
     * @return the unchanged valid reference
     * @throws IllegalArgumentException if the value does not match the required format
     */
    private static String validateReference(
        String reference,
        String expectedPrefix,
        String fieldName
    ) {
        if (!ReferenceFormat.matches(reference, expectedPrefix)) {
            throw new IllegalArgumentException(
                fieldName + " does not match the expected reference format"
            );
        }
        return reference;
    }

    /**
     * Strips surrounding whitespace and validates the business name's presence and length.
     *
     * @param name input business name
     * @return the normalized name
     * @throws OrganizationException with {@code INVALID_NAME} for null, blank or oversized input
     */
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

    /**
     * Builds the domain failure for a name that violates presence or length constraints.
     *
     * @return an {@code INVALID_NAME} exception
     */
    private static OrganizationException invalidName() {
        return new OrganizationException(
            OrganizationErrorCode.INVALID_NAME,
            "name must not be null or blank and must not exceed "
                + MAX_NAME_LENGTH + " characters"
        );
    }

    /**
     * Resolves a timezone identifier without silently correcting its value.
     *
     * @param timezone identifier recognized by {@link ZoneId}
     * @return the resolved timezone
     * @throws OrganizationException with {@code INVALID_TIMEZONE} if null or unrecognized
     */
    private static ZoneId validateTimezone(String timezone) {
        if (timezone == null) {
            throw invalidTimezone();
        }

        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new OrganizationException(
                OrganizationErrorCode.INVALID_TIMEZONE,
                "timezone must be a valid ZoneId",
                exception
            );
        }
    }

    /**
     * Builds the domain failure for an absent timezone identifier.
     *
     * @return an {@code INVALID_TIMEZONE} exception
     */
    private static OrganizationException invalidTimezone() {
        return new OrganizationException(
            OrganizationErrorCode.INVALID_TIMEZONE,
            "timezone must be a valid ZoneId"
        );
    }
}
