package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class ReplaceAdministrator {

        @Test
        void replacesTheAdministratorWithoutChangingOtherTeamState() {
            Team team = activeTeam();
            givenTeam(team);
            when(teamRepository.update(team)).thenReturn(team);
            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

            Team result = service.replaceAdministrator(tenantContext(), command(REPLACEMENT_REFERENCE));

            verify(teamRepository).update(teamCaptor.capture());
            assertSame(team, result);
            assertEquals(REPLACEMENT_REFERENCE, teamCaptor.getValue().getAdminReference());
            assertEquals(MANAGER_REFERENCE, teamCaptor.getValue().getManagerReference());
            assertEquals(TeamStatus.ACTIVE, teamCaptor.getValue().getStatus());
            verify(responsibleUsersValidator)
                    .validateOperationalAdministrator(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);
        }

        @Test
        void stopsWhenTheReplacementAdministratorIsRejected() {
            Team team = activeTeam();
            givenTeam(team);
            TeamException failure =
                    new TeamException(TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE, "Team administrator is not available");
            doThrow(failure)
                    .when(responsibleUsersValidator)
                    .validateOperationalAdministrator(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> service.replaceAdministrator(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertSame(failure, exception);
            assertEquals(ADMINISTRATOR_REFERENCE, team.getAdminReference());
            verify(teamRepository, never()).update(any(Team.class));
        }

        @Test
        void returnsTheSameTeamWithoutDirectoryLookupWhenTheAdministratorIsUnchanged() {
            Team team = activeTeam();
            givenTeam(team);

            Team result = service.replaceAdministrator(tenantContext(), command(ADMINISTRATOR_REFERENCE));

            assertSame(team, result);
            verifyNoInteractions(responsibleUsersValidator);
            verify(teamRepository, never()).update(any(Team.class));
        }

        @Test
        void rejectsReplacementOnAnArchivedTeamBeforeCheckingTheUser() {
            givenTeam(archivedTeam());

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> service.replaceAdministrator(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
            verifyNoInteractions(responsibleUsersValidator);
            verify(teamRepository, never()).update(any(Team.class));
        }

        @Test
        void reportsWhenTheTeamIsMissingFromTheCurrentTenant() {
            givenTeamNotFound();

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> service.replaceAdministrator(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(responsibleUsersValidator);
            verify(teamRepository, never()).update(any(Team.class));
        }
    }

    @Nested
    class ReplaceManager {

        @Test
        void replacesTheManagerWithoutChangingOtherTeamState() {
            Team team = suspendedTeam();
            givenTeam(team);
            when(teamRepository.update(team)).thenReturn(team);

            Team result = service.replaceManager(tenantContext(), command(REPLACEMENT_REFERENCE));

            assertSame(team, result);
            assertEquals(ADMINISTRATOR_REFERENCE, team.getAdminReference());
            assertEquals(REPLACEMENT_REFERENCE, team.getManagerReference());
            assertEquals(TeamStatus.SUSPENDED, team.getStatus());
            verify(responsibleUsersValidator).validateOperationalManager(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);
            verify(teamRepository).update(team);
        }

        @Test
        void stopsWhenTheReplacementManagerIsRejected() {
            Team team = activeTeam();
            givenTeam(team);
            TeamException failure =
                    new TeamException(TeamErrorCode.MANAGER_NOT_AVAILABLE, "Team manager is not available");
            doThrow(failure)
                    .when(responsibleUsersValidator)
                    .validateOperationalManager(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);

            TeamException exception = assertThrows(
                    TeamException.class, () -> service.replaceManager(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertSame(failure, exception);
            assertEquals(MANAGER_REFERENCE, team.getManagerReference());
            verify(teamRepository, never()).update(any(Team.class));
        }

        @Test
        void returnsTheSameTeamWithoutDirectoryLookupWhenTheManagerIsUnchanged() {
            Team team = suspendedTeam();
            givenTeam(team);

            Team result = service.replaceManager(tenantContext(), command(MANAGER_REFERENCE));

            assertSame(team, result);
            verifyNoInteractions(responsibleUsersValidator);
            verify(teamRepository, never()).update(any(Team.class));
        }

        @Test
        void rejectsReplacementOnAnArchivedTeamBeforeCheckingTheUser() {
            givenTeam(archivedTeam());

            TeamException exception = assertThrows(
                    TeamException.class, () -> service.replaceManager(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
            verifyNoInteractions(responsibleUsersValidator);
            verify(teamRepository, never()).update(any(Team.class));
        }

        @Test
        void reportsWhenTheTeamIsMissingFromTheCurrentTenant() {
            givenTeamNotFound();

            TeamException exception = assertThrows(
                    TeamException.class, () -> service.replaceManager(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(responsibleUsersValidator);
            verify(teamRepository, never()).update(any(Team.class));
        }
    }

    private static TenantContext tenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private static TeamResponsibleCommand command(String responsibleReference) {
        return new TeamResponsibleCommand(TEAM_REFERENCE, responsibleReference);
    }

    private static Team activeTeam() {
        return team(TeamStatus.ACTIVE);
    }

    private static Team suspendedTeam() {
        return team(TeamStatus.SUSPENDED);
    }

    private static Team archivedTeam() {
        return team(TeamStatus.ARCHIVED);
    }

    private static Team team(TeamStatus status) {
        return Team.restore(
                TEAM_ID,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "Platform",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                status);
    }

    private void givenTeam(Team team) {
        when(teamRepository.findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.of(team));
    }

    private void givenTeamNotFound() {
        when(teamRepository.findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.empty());
    }
}
