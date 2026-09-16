package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamResponsibleUsersServiceTest {

    private static final Long TEAM_ID = 42L;
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B900";
    private static final String TEAM_REFERENCE = "TEM-2026-1309-00000ZA7B901";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-1309-00000ZA7B902";
    private static final String MANAGER_REFERENCE = "USR-2026-1309-00000ZA7B903";
    private static final String REPLACEMENT_REFERENCE = "USR-2026-1309-00000ZA7B904";

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamResponsibleUsersValidator responsibleUsersValidator;

    @InjectMocks
    private TeamResponsibleUsersService service;

    @Test
    void replacesTheAdministratorWithoutChangingTheTeamStatus() {
        Team team = activeTeam();
        givenTeam(team);
        when(teamRepository.update(team)).thenReturn(team);
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

        Team result = service.replaceAdministrator(
                tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE));

        verify(teamRepository).update(teamCaptor.capture());
        assertSame(team, result);
        assertEquals(REPLACEMENT_REFERENCE, teamCaptor.getValue().getAdminReference());
        assertEquals(MANAGER_REFERENCE, teamCaptor.getValue().getManagerReference());
        assertEquals(TeamStatus.ACTIVE, teamCaptor.getValue().getStatus());
        verify(responsibleUsersValidator)
                .validateOperationalAdministrator(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);
    }

    @Test
    void replacesTheManagerWithoutChangingTheTeamStatus() {
        Team team = suspendedTeam();
        givenTeam(team);
        when(teamRepository.update(team)).thenReturn(team);

        Team result = service.replaceManager(
                tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE));

        assertSame(team, result);
        assertEquals(ADMINISTRATOR_REFERENCE, team.getAdminReference());
        assertEquals(REPLACEMENT_REFERENCE, team.getManagerReference());
        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
        verify(responsibleUsersValidator).validateOperationalManager(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);
        verify(teamRepository).update(team);
    }

    @Test
    void doesNotWriteWhenTheNewAdministratorIsRejected() {
        Team team = activeTeam();
        givenTeam(team);
        TeamException validationFailure =
                new TeamException(TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE, "Team administrator is not available");
        doThrow(validationFailure)
                .when(responsibleUsersValidator)
                .validateOperationalAdministrator(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);

        TeamException exception = assertThrows(
                TeamException.class,
                () -> service.replaceAdministrator(
                        tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE)));

        assertSame(validationFailure, exception);
        assertEquals(ADMINISTRATOR_REFERENCE, team.getAdminReference());
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void returnsAnUnchangedTeamWithoutDirectoryLookupWhenTheAdministratorIsUnchanged() {
        Team team = activeTeam();
        givenTeam(team);

        Team result = service.replaceAdministrator(
                tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, ADMINISTRATOR_REFERENCE));

        assertSame(team, result);
        verifyNoInteractions(responsibleUsersValidator);
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void returnsAnUnchangedTeamWithoutDirectoryLookupWhenTheManagerIsUnchanged() {
        Team team = suspendedTeam();
        givenTeam(team);

        Team result =
                service.replaceManager(tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, MANAGER_REFERENCE));

        assertSame(team, result);
        verifyNoInteractions(responsibleUsersValidator);
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void rejectsTheReplacementWhenTheTeamIsArchived() {
        Team team = archivedTeam();
        givenTeam(team);

        TeamException exception = assertThrows(
                TeamException.class,
                () -> service.replaceManager(
                        tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE)));

        assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verifyNoInteractions(responsibleUsersValidator);
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void rejectsAdministratorReplacementWhenTheTeamIsArchived() {
        Team team = archivedTeam();
        givenTeam(team);

        TeamException exception = assertThrows(
                TeamException.class,
                () -> service.replaceAdministrator(
                        tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE)));

        assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verifyNoInteractions(responsibleUsersValidator);
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void doesNotWriteWhenTheTeamIsMissingFromTheCurrentTenant() {
        when(teamRepository.findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.empty());

        TeamException exception = assertThrows(
                TeamException.class,
                () -> service.replaceManager(
                        tenantContext(), new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE)));

        assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(responsibleUsersValidator);
        verify(teamRepository, never()).update(any(Team.class));
    }

    private TenantContext tenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private Team activeTeam() {
        return Team.restore(
                TEAM_ID,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "Platform",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                TeamStatus.ACTIVE);
    }

    private Team suspendedTeam() {
        return Team.restore(
                TEAM_ID,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "Platform",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                TeamStatus.SUSPENDED);
    }

    private Team archivedTeam() {
        return Team.restore(
                TEAM_ID,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "Platform",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                TeamStatus.ARCHIVED);
    }

    private void givenTeam(Team team) {
        when(teamRepository.findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.of(team));
    }
}
