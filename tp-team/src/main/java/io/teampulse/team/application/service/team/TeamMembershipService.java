package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.application.port.in.team.TeamMemberCommand;
import io.teampulse.team.application.port.in.team.TeamMembershipUseCase;
import io.teampulse.team.application.port.out.team.TeamMemberRepository;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class TeamMembershipService implements TeamMembershipUseCase {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamOrganizationAvailabilityValidator organizationAvailabilityValidator;
    private final TeamMemberUserAvailabilityValidator memberUserAvailabilityValidator;
    private final Clock clock;

    @Override
    @Transactional
    public TeamMember addMember(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findActiveTeam(tenantContext, command);
        ensureNoCurrentMembership(team, command.userReference());
        organizationAvailabilityValidator.validateAvailable(team.getOrganizationReference());
        memberUserAvailabilityValidator.validateAvailable(team.getOrganizationReference(), command.userReference());

        return teamMemberRepository.create(
                TeamMember.add(team.getOrganizationReference(), team.getId(), command.userReference(), now()));
    }

    @Override
    @Transactional
    public TeamMember inviteMember(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findActiveTeam(tenantContext, command);
        ensureNoCurrentMembership(team, command.userReference());
        organizationAvailabilityValidator.validateAvailable(team.getOrganizationReference());
        memberUserAvailabilityValidator.validateAvailableOrPending(
                team.getOrganizationReference(), command.userReference());

        return teamMemberRepository.create(
                TeamMember.invite(team.getOrganizationReference(), team.getId(), command.userReference()));
    }

    @Override
    @Transactional
    public TeamMember activateMember(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findActiveTeam(tenantContext, command);
        TeamMember member = findCurrentMembership(team, command.userReference());
        organizationAvailabilityValidator.validateAvailable(team.getOrganizationReference());
        memberUserAvailabilityValidator.validateAvailable(team.getOrganizationReference(), command.userReference());
        member.activate(now());

        return teamMemberRepository.update(member);
    }

    @Override
    @Transactional
    public TeamMember suspendMember(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findMutableTeam(tenantContext, command);
        TeamMember member = findCurrentMembership(team, command.userReference());
        member.suspend();

        return teamMemberRepository.update(member);
    }

    @Override
    @Transactional
    public TeamMember reactivateMember(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findActiveTeam(tenantContext, command);
        TeamMember member = findCurrentMembership(team, command.userReference());
        organizationAvailabilityValidator.validateAvailable(team.getOrganizationReference());
        memberUserAvailabilityValidator.validateAvailable(team.getOrganizationReference(), command.userReference());
        member.reactivate();

        return teamMemberRepository.update(member);
    }

    @Override
    @Transactional
    public TeamMember removeMember(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findMutableTeam(tenantContext, command);
        TeamMember member = findCurrentMembership(team, command.userReference());
        member.remove(now());

        return teamMemberRepository.update(member);
    }

    private Team findActiveTeam(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findTeam(tenantContext.tenantReference(), command.teamReference());
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw teamUnavailable(command.teamReference());
        }
        return team;
    }

    private Team findMutableTeam(TenantContext tenantContext, TeamMemberCommand command) {
        Team team = findTeam(tenantContext.tenantReference(), command.teamReference());
        if (team.getStatus() == TeamStatus.ARCHIVED) {
            throw teamUnavailable(command.teamReference());
        }
        return team;
    }

    private Team findTeam(String organizationReference, String teamReference) {
        return teamRepository
                .findByReference(organizationReference, teamReference)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_FOUND, "Team was not found: " + teamReference));
    }

    private TeamMember findCurrentMembership(Team team, String userReference) {
        return teamMemberRepository
                .findCurrent(team.getOrganizationReference(), team.getId(), userReference)
                .orElseThrow(() -> new TeamException(TeamErrorCode.MEMBER_NOT_FOUND, "Team member was not found"));
    }

    private void ensureNoCurrentMembership(Team team, String userReference) {
        if (teamMemberRepository
                .findCurrent(team.getOrganizationReference(), team.getId(), userReference)
                .isPresent()) {
            throw new TeamException(TeamErrorCode.MEMBER_ALREADY_EXISTS, "Team member already exists");
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private static TeamException teamUnavailable(String teamReference) {
        return new TeamException(TeamErrorCode.TEAM_UNAVAILABLE, "Team is unavailable: " + teamReference);
    }
}
