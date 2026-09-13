package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * Validates organization responsible users against the identity module at the
 * execution time of an application use case.
 *
 * <p>This component performs point-in-time checks only. It does not monitor
 * responsible users after the organization operation has completed.</p>
 */
@Service
@Validated
@AllArgsConstructor
public class OrganizationResponsibleUsersValidator {

    private final UserDirectory userDirectory;

    /**
     * Validates that a user can be assigned as an administrator candidate.
     * {@code AVAILABLE} and {@code PENDING} users are accepted; the returned
     * availability lets the application service decide whether activation can
     * be attempted.
     *
     * @param organizationReference organization whose responsible user is checked
     * @param administratorReference candidate administrator reference
     * @return the current user availability
     * @throws OrganizationException when the user is not found, unavailable, or
     *         the identity directory cannot be reached
     */
    public UserAvailability validateAssignableAdministrator(
        String organizationReference,
        String administratorReference
    ) {
        return validateAssignable(
            organizationReference,
            administratorReference,
            ResponsibleRole.ADMINISTRATOR
        );
    }

    /**
     * Validates that a user can be assigned as a manager candidate.
     * {@code AVAILABLE} and {@code PENDING} users are accepted; the returned
     * availability lets the application service decide whether activation or
     * reactivation can be attempted.
     *
     * @param organizationReference organization whose responsible user is checked
     * @param managerReference candidate manager reference
     * @return the current user availability
     * @throws OrganizationException when the user is not found, unavailable, or
     *         the identity directory cannot be reached
     */
    public UserAvailability validateAssignableManager(
        String organizationReference,
        String managerReference
    ) {
        return validateAssignable(
            organizationReference,
            managerReference,
            ResponsibleRole.MANAGER
        );
    }

    /**
     * Validates that the administrator is operational for an organization
     * activation, reactivation, or direct replacement. Only {@code AVAILABLE}
     * users are accepted.
     *
     * @param organizationReference organization whose administrator is checked
     * @param administratorReference administrator reference
     * @throws OrganizationException when the user is not found, not available,
     *         or the identity directory cannot be reached
     */
    public void validateOperationalAdministrator(
        String organizationReference,
        String administratorReference
    ) {
        validateOperational(
            checkAvailability(organizationReference, administratorReference),
            ResponsibleRole.ADMINISTRATOR
        );
    }

    /**
     * Validates that the manager is operational for an organization activation,
     * reactivation, or direct replacement. Only {@code AVAILABLE} users are
     * accepted.
     *
     * @param organizationReference organization whose manager is checked
     * @param managerReference manager reference
     * @throws OrganizationException when the user is not found, not available,
     *         or the identity directory cannot be reached
     */
    public void validateOperationalManager(
        String organizationReference,
        String managerReference
    ) {
        validateOperational(
            checkAvailability(organizationReference, managerReference),
            ResponsibleRole.MANAGER
        );
    }

    /**
     * Validates both operational responsible users in one point-in-time check.
     * When the administrator and manager references are identical, the single
     * directory result is reused for both roles and only one lookup is made.
     *
     * @param organizationReference organization whose responsible users are checked
     * @param administratorReference administrator reference
     * @param managerReference manager reference
     * @throws OrganizationException when either user is not found, not available,
     *         or the identity directory cannot be reached
     */
    public void validateOperationalResponsibleUsers(
        String organizationReference,
        String administratorReference,
        String managerReference
    ) {
        UserAvailability administratorAvailability = checkAvailability(
            organizationReference,
            administratorReference
        );
        validateOperational(
            administratorAvailability,
            ResponsibleRole.ADMINISTRATOR
        );

        // A shared responsible user must be checked once and mapped to both roles.
        if (administratorReference.equals(managerReference)) {
            return;
        }

        validateOperational(
            checkAvailability(organizationReference, managerReference),
            ResponsibleRole.MANAGER
        );
    }

    /**
     * Checks whether both responsible users are currently available without
     * turning a non-available state into a business error. This probe is used
     * only to decide whether an assignment may trigger activation or
     * reactivation.
     *
     * @param organizationReference organization whose responsible users are checked
     * @param administratorReference administrator reference
     * @param managerReference manager reference
     * @return {@code true} only when both users are {@code AVAILABLE}
     * @throws OrganizationException when the identity directory cannot be reached
     */
    public boolean areResponsibleUsersAvailable(
        String organizationReference,
        String administratorReference,
        String managerReference
    ) {
        UserAvailability administratorAvailability = checkAvailability(
            organizationReference,
            administratorReference
        );
        if (administratorAvailability != UserAvailability.AVAILABLE) {
            return false;
        }

        UserAvailability managerAvailability = Objects.equals(
            administratorReference,
            managerReference
        )
            ? administratorAvailability
            : checkAvailability(organizationReference, managerReference);

        return managerAvailability == UserAvailability.AVAILABLE;
    }

    private UserAvailability validateAssignable(
        String organizationReference,
        String userReference,
        ResponsibleRole role
    ) {
        UserAvailability availability = checkAvailability(
            organizationReference,
            userReference
        );

        return switch (availability) {
            case AVAILABLE, PENDING -> availability;
            case UNAVAILABLE -> throw role.notAvailable();
            case NOT_FOUND -> throw role.notFound();
        };
    }

    private void validateOperational(
        UserAvailability availability,
        ResponsibleRole role
    ) {
        switch (availability) {
            case AVAILABLE -> {
                return;
            }
            case PENDING, UNAVAILABLE -> throw role.notAvailable();
            case NOT_FOUND -> throw role.notFound();
        }
    }

    /**
     * Performs the point-in-time directory lookup and translates technical
     * directory failures into the organization error vocabulary.
     */
    private UserAvailability checkAvailability(
        String organizationReference,
        String userReference
    ) {
        try {
            return userDirectory.check(organizationReference, userReference);
        } catch (UserDirectoryException exception) {
            throw new OrganizationException(
                OrganizationErrorCode.USER_DIRECTORY_UNAVAILABLE,
                "Unable to validate organization responsible user",
                exception
            );
        }
    }

    @AllArgsConstructor
    private enum ResponsibleRole {
        ADMINISTRATOR(
            OrganizationErrorCode.ADMINISTRATOR_NOT_FOUND,
            OrganizationErrorCode.ADMINISTRATOR_NOT_AVAILABLE,
            "administrator"
        ),
        MANAGER(
            OrganizationErrorCode.MANAGER_NOT_FOUND,
            OrganizationErrorCode.MANAGER_NOT_AVAILABLE,
            "manager"
        );

        private final OrganizationErrorCode notFoundErrorCode;
        private final OrganizationErrorCode notAvailableErrorCode;
        private final String label;

        private OrganizationException notFound() {
            return new OrganizationException(
                notFoundErrorCode,
                "Organization " + label + " was not found"
            );
        }

        private OrganizationException notAvailable() {
            return new OrganizationException(
                notAvailableErrorCode,
                "Organization " + label + " is not available"
            );
        }
    }
}
