package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.api.TeamLifecycleState;
import io.teampulse.team.api.TeamResponsibility;
import io.teampulse.team.api.TeamResponsibilityDirectory;
import io.teampulse.team.api.TeamResponsibilityDirectoryException;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.model.Team;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class TeamResponsibilityDirectoryService implements TeamResponsibilityDirectory {

    private final TeamRepository teamRepository;

    @Override
    public List<TeamResponsibility> findByResponsibleUser(TenantContext tenantContext, String userReference) {
        try {
            return teamRepository.findByResponsibleUser(tenantContext.tenantReference(), userReference).stream()
                    .map(TeamResponsibilityDirectoryService::toResponsibility)
                    .toList();
        } catch (RuntimeException exception) {
            throw new TeamResponsibilityDirectoryException(exception);
        }
    }

    private static TeamResponsibility toResponsibility(Team team) {
        return new TeamResponsibility(team.getReference(), toPublicState(team));
    }

    private static TeamLifecycleState toPublicState(Team team) {
        return switch (team.getStatus()) {
            case ACTIVE -> TeamLifecycleState.ACTIVE;
            case SUSPENDED -> TeamLifecycleState.SUSPENDED;
            case ARCHIVED -> TeamLifecycleState.ARCHIVED;
        };
    }
}
