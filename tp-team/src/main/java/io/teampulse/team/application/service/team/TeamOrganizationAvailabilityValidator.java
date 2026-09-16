package io.teampulse.team.application.service.team;

import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Validates that the organisation of a team is operational when an operation
 * opens or restores member access.
 */
@Service
@Validated
@AllArgsConstructor
public class TeamOrganizationAvailabilityValidator {

    private final OrganizationDirectory organizationDirectory;

    public void validateAvailable(String organizationReference) {
        OrganizationAvailability availability;
        try {
            availability = organizationDirectory.check(organizationReference);
        } catch (OrganizationDirectoryException exception) {
            throw new TeamException(
                    TeamErrorCode.ORGANIZATION_DIRECTORY_UNAVAILABLE,
                    "Unable to validate team organization",
                    exception);
        }

        switch (availability) {
            case AVAILABLE -> {}
            case UNAVAILABLE ->
                throw new TeamException(TeamErrorCode.ORGANIZATION_UNAVAILABLE, "Team organization is not available");
            case NOT_FOUND ->
                throw new TeamException(TeamErrorCode.ORGANIZATION_NOT_FOUND, "Team organization was not found");
        }
    }
}
