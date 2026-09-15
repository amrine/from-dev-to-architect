package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
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
    private final TeamOrganizationAvailabilityValidator organizationAvailabilityValidator;
    private final TeamResponsibleUsersValidator responsibleUsersValidator;

    @Override
    @Transactional
    public Team create(TenantContext tenantContext, CreateTeamCommand command) {
        String organizationReference = tenantContext.tenantReference();
        organizationAvailabilityValidator.validateAvailable(organizationReference);
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
        organizationAvailabilityValidator.validateAvailable(organizationReference);
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
}
