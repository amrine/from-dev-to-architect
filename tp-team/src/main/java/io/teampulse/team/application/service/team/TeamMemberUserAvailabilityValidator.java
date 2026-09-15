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
 * Validates user availability according to the membership operation being
 * executed.
 */
@Service
@Validated
@AllArgsConstructor
public class TeamMemberUserAvailabilityValidator {

    private final UserDirectory userDirectory;

    public void validateAvailable(String organizationReference, String userReference) {
        validateAvailability(checkAvailability(organizationReference, userReference), false);
    }

    public void validateAvailableOrPending(String organizationReference, String userReference) {
        validateAvailability(checkAvailability(organizationReference, userReference), true);
    }

    private UserAvailability checkAvailability(String organizationReference, String userReference) {
        try {
            return userDirectory.check(organizationReference, userReference);
        } catch (UserDirectoryException exception) {
            throw new TeamException(
                    TeamErrorCode.USER_DIRECTORY_UNAVAILABLE, "Unable to validate team member user", exception);
        }
    }

    private static void validateAvailability(UserAvailability availability, boolean pendingAllowed) {
        switch (availability) {
            case AVAILABLE -> {}
            case PENDING -> {
                if (pendingAllowed) {
                    return;
                }
                throw new TeamException(TeamErrorCode.MEMBER_USER_NOT_AVAILABLE, "Team member user is not available");
            }
            case NOT_FOUND ->
                throw new TeamException(TeamErrorCode.MEMBER_USER_NOT_FOUND, "Team member user was not found");
            case UNAVAILABLE ->
                throw new TeamException(TeamErrorCode.MEMBER_USER_NOT_AVAILABLE, "Team member user is not available");
        }
    }
}
