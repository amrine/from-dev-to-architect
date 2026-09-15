package io.teampulse.team.application.service.team;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Validates team responsible users against the identity module at the execution
 * time of an application use case.
 */
@Service
@Validated
@AllArgsConstructor
public class TeamResponsibleUsersValidator {

    private final UserDirectory userDirectory;

    /**
     * Validates both team responsible users in one point-in-time check. When
     * both responsibilities belong to the same user, the directory is queried
     * only once.
     */
    public void validateOperationalResponsibleUsers(
            String organizationReference, String administratorReference, String managerReference) {
        UserAvailability administratorAvailability = checkAvailability(organizationReference, administratorReference);
        validateOperational(administratorAvailability, ResponsibleRole.ADMINISTRATOR);

        if (administratorReference.equals(managerReference)) {
            return;
        }

        validateOperational(checkAvailability(organizationReference, managerReference), ResponsibleRole.MANAGER);
    }

    private UserAvailability checkAvailability(String organizationReference, String userReference) {
        try {
            return userDirectory.check(organizationReference, userReference);
        } catch (UserDirectoryException exception) {
            throw new TeamException(
                    TeamErrorCode.USER_DIRECTORY_UNAVAILABLE, "Unable to validate team responsible user", exception);
        }
    }

    private static void validateOperational(UserAvailability availability, ResponsibleRole role) {
        switch (availability) {
            case AVAILABLE -> {}
            case PENDING, UNAVAILABLE -> throw role.notAvailable();
            case NOT_FOUND -> throw role.notFound();
        }
    }

    @AllArgsConstructor
    private enum ResponsibleRole {
        ADMINISTRATOR(
                TeamErrorCode.ADMINISTRATOR_NOT_FOUND, TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE, "administrator"),
        MANAGER(TeamErrorCode.MANAGER_NOT_FOUND, TeamErrorCode.MANAGER_NOT_AVAILABLE, "manager");

        private final TeamErrorCode notFoundErrorCode;
        private final TeamErrorCode notAvailableErrorCode;
        private final String label;

        private TeamException notFound() {
            return new TeamException(notFoundErrorCode, "Team " + label + " was not found");
        }

        private TeamException notAvailable() {
            return new TeamException(notAvailableErrorCode, "Team " + label + " is not available");
        }
    }
}
