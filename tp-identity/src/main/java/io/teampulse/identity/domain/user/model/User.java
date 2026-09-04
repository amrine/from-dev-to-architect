package io.teampulse.identity.domain.user.model;

import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import lombok.Getter;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
public final class User {

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_NAME_LENGTH = 100;

    private static final Pattern USER_REFERENCE_PATTERN = Pattern.compile(
        "USR-[0-9]{4}-[0-9]{4}-[0-9A-Z]{12}"
    );

    private static final Pattern ORGANIZATION_REFERENCE_PATTERN = Pattern.compile(
        "ORG-[0-9]{4}-[0-9]{4}-[0-9A-Z]{12}"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[^\\s@]+@[^\\s@]+",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private final String reference;
    private final String organizationReference;
    private final String email;
    private final String firstName;
    private final String lastName;
    private UserStatus status;

    private User(
        String reference,
        String organizationReference,
        String email,
        String firstName,
        String lastName,
        UserStatus status
    ) {
        this.reference = validateReference(reference, USER_REFERENCE_PATTERN, "reference");
        this.organizationReference = validateReference(organizationReference, ORGANIZATION_REFERENCE_PATTERN, "organizationReference");
        this.email = validateEmail(email);
        this.firstName = validateName(firstName, UserErrorCode.INVALID_FIRST_NAME, "firstName");
        this.lastName = validateName(lastName, UserErrorCode.INVALID_LAST_NAME, "lastName");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * Creates a new user through the direct-creation flow.
     * The user starts in {@link UserStatus#CREATING}. Technical persistence
     * identity, version and audit data remain outside this domain model.
     */
    public static User create(
        String reference,
        String organizationReference,
        String email,
        String firstName,
        String lastName
    ) {
        return createNew(reference, organizationReference, email, firstName, lastName, UserStatus.CREATING);
    }

    /**
     * Creates a new user through the invitation flow.
     * The user starts in {@link UserStatus#INVITED}. Technical persistence
     * identity, version and audit data remain outside this domain model.
     */
    public static User invite(
        String reference,
        String organizationReference,
        String email,
        String firstName,
        String lastName
    ) {
        return createNew(reference, organizationReference, email, firstName, lastName, UserStatus.INVITED);
    }

    /**
     * Reconstitutes the business state of a previously persisted user without
     * applying a creation workflow. Technical persistence state remains on the
     * persistence entity.
     */
    public static User restore(
        String reference,
        String organizationReference,
        String email,
        String firstName,
        String lastName,
        UserStatus status
    ) {
        return new User(
            reference,
            organizationReference,
            email,
            firstName,
            lastName,
            status
        );
    }

    private static User createNew(
        String reference,
        String organizationReference,
        String email,
        String firstName,
        String lastName,
        UserStatus status
    ) {
        return new User(
            reference,
            organizationReference,
            email,
            firstName,
            lastName,
            status
        );
    }

    public void startCreation() {
        transitionTo(UserStatus.CREATING);
    }

    public void activate() {
        transitionTo(UserStatus.ACTIVE);
    }

    public void suspend() {
        transitionTo(UserStatus.SUSPENDED);
    }

    public void deactivate() {
        transitionTo(UserStatus.DEACTIVATED);
    }

    private void transitionTo(UserStatus target) {
        status.validateTransitionTo(target);
        status = target;
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

    private static String validateEmail(String email) {
        if (email == null) {
            throw invalidEmail();
        }

        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (
            normalizedEmail.length() > MAX_EMAIL_LENGTH
                || !EMAIL_PATTERN.matcher(normalizedEmail).matches()
        ) {
            throw invalidEmail();
        }
        return normalizedEmail;
    }

    private static UserException invalidEmail() {
        return new UserException(
            UserErrorCode.INVALID_EMAIL,
            "email must contain a local part and a domain without whitespace "
                + "and must not exceed " + MAX_EMAIL_LENGTH + " characters"
        );
    }

    private static String validateName(
        String name,
        UserErrorCode errorCode,
        String fieldName
    ) {
        if (name == null) {
            throw new UserException(
                errorCode,
                fieldName + " must not be null or blank and must not exceed "
                    + MAX_NAME_LENGTH + " characters"
            );
        }

        String normalizedName = name.strip();
        if (normalizedName.isBlank() || normalizedName.length() > MAX_NAME_LENGTH) {
            throw new UserException(
                errorCode,
                fieldName + " must not be null or blank and must not exceed "
                    + MAX_NAME_LENGTH + " characters"
            );
        }
        return normalizedName;
    }
}
