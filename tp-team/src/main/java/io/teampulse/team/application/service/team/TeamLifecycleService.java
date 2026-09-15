package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class TeamLifecycleService implements TeamLifecycleUseCase {

    private static final String TEAM_REFERENCE_PREFIX = "TEM";

    private final TeamRepository teamRepository;
    private final ReferenceFactory referenceFactory;
    private final OrganizationDirectory organizationDirectory;
    private final TeamResponsibleUsersValidator responsibleUsersValidator;

    @Override
    @Transactional
    public Team create(TenantContext tenantContext, CreateTeamCommand command) {
        String organizationReference = tenantContext.tenantReference();
        validateOrganizationAvailability(organizationReference);
        responsibleUsersValidator.validateOperationalResponsibleUsers(
                organizationReference, command.adminReference(), command.managerReference());

        Team team = Team.create(
                referenceFactory.generate(TEAM_REFERENCE_PREFIX),
                organizationReference,
                command.name(),
                command.adminReference(),
                command.managerReference());

        return teamRepository.create(team);
    }

    @Override
    @Transactional
    public Team suspend(TenantContext tenantContext, String teamReference) {
        Team team = findTeam(tenantContext.tenantReference(), teamReference);
        team.suspend();

        return teamRepository.update(team);
    }

    @Override
    @Transactional
    public Team reactivate(TenantContext tenantContext, String teamReference) {
        String organizationReference = tenantContext.tenantReference();
        Team team = findTeam(organizationReference, teamReference);
        team.getStatus().validateTransitionTo(TeamStatus.ACTIVE);
        validateOrganizationAvailability(organizationReference);
        responsibleUsersValidator.validateOperationalResponsibleUsers(
                organizationReference, team.getAdminReference(), team.getManagerReference());
        team.reactivate();

        return teamRepository.update(team);
    }

    @Override
    @Transactional
    public Team archive(TenantContext tenantContext, String teamReference) {
        Team team = findTeam(tenantContext.tenantReference(), teamReference);
        team.archive();

        return teamRepository.update(team);
    }

    private Team findTeam(String organizationReference, String teamReference) {
        return teamRepository
                .findByReference(organizationReference, teamReference)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_FOUND, "Team was not found: " + teamReference));
    }

    private void validateOrganizationAvailability(String organizationReference) {
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
