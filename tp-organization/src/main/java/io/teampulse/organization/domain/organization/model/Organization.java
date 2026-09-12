package io.teampulse.organization.domain.organization.model;

import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import lombok.Getter;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
public final class Organization {

    private static final int MAX_NAME_LENGTH = 200;

    private static final Pattern ORGANIZATION_REFERENCE_PATTERN = Pattern.compile(
        "ORG-[0-9]{4}-[0-9]{4}-[0-9A-Z]{12}"
    );

    private static final Pattern USER_REFERENCE_PATTERN = Pattern.compile(
        "USR-[0-9]{4}-[0-9]{4}-[0-9A-Z]{12}"
    );

    private final String reference;
    private final String name;
    private final ZoneId timezone;
    private String adminReference;
    private String managerReference;
    private OrganizationStatus status;

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
            ORGANIZATION_REFERENCE_PATTERN,
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
        validateResponsibleStructure();
    }

    /**
     * Creates an organization in {@link OrganizationStatus#CREATING} without
     * responsible users. Technical persistence identity, version and audit
     * data remain outside this domain model.
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

    private void validateResponsibleStructure() {
        boolean requirementsNotMet = switch (status) {
            case ACTIVE -> adminReference == null || managerReference == null;
            case SUSPENDED -> adminReference == null;
            case CREATING, ARCHIVED -> false;
        };

        if (requirementsNotMet) {
            throw new OrganizationException(
                OrganizationErrorCode.ACTIVATION_REQUIREMENTS_NOT_MET,
                "Organization status %s does not meet responsible user requirements"
                    .formatted(status)
            );
        }
    }

    private static String validateOptionalUserReference(
        String reference,
        String fieldName
    ) {
        if (reference == null) {
            return null;
        }
        return validateReference(reference, USER_REFERENCE_PATTERN, fieldName);
    }

    private static String validateReference(
        String reference,
        Pattern expectedPattern,
        String fieldName
    ) {
        if (reference == null || !expectedPattern.matcher(reference).matches()) {
            throw new IllegalArgumentException(
                fieldName + " does not match the expected reference format"
            );
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

    private static OrganizationException invalidName() {
        return new OrganizationException(
            OrganizationErrorCode.INVALID_NAME,
            "name must not be null or blank and must not exceed "
                + MAX_NAME_LENGTH + " characters"
        );
    }

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

    private static OrganizationException invalidTimezone() {
        return new OrganizationException(
            OrganizationErrorCode.INVALID_TIMEZONE,
            "timezone must be a valid ZoneId"
        );
    }
}
