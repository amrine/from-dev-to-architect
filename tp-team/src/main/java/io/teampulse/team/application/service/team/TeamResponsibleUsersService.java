package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.application.port.in.team.TeamResponsibleUsersUseCase;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class TeamResponsibleUsersService implements TeamResponsibleUsersUseCase {

    private final TeamRepository teamRepository;
    private final TeamResponsibleUsersValidator responsibleUsersValidator;

    @Override
    @Transactional
    public Team replaceAdministrator(TenantContext tenantContext, TeamResponsibleCommand command) {
        Team team = findTeam(tenantContext.tenantReference(), command.teamReference());
        String administratorReference = command.responsibleReference();

        if (Objects.equals(team.getAdminReference(), administratorReference)) {
            return team;
        }

        team.validateReplacementAllowed("replaceAdministrator");
        responsibleUsersValidator.validateOperationalAdministrator(
                team.getOrganizationReference(), administratorReference);
        team.replaceAdministrator(administratorReference);

        return teamRepository.update(team);
    }

    @Override
    @Transactional
    public Team replaceManager(TenantContext tenantContext, TeamResponsibleCommand command) {
        Team team = findTeam(tenantContext.tenantReference(), command.teamReference());
        String managerReference = command.responsibleReference();

        if (Objects.equals(team.getManagerReference(), managerReference)) {
            return team;
        }

        team.validateReplacementAllowed("replaceManager");
        responsibleUsersValidator.validateOperationalManager(team.getOrganizationReference(), managerReference);
        team.replaceManager(managerReference);

        return teamRepository.update(team);
    }

    private Team findTeam(String organizationReference, String teamReference) {
        return teamRepository
                .findByReference(organizationReference, teamReference)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_FOUND, "Team was not found: " + teamReference));
    }
}
