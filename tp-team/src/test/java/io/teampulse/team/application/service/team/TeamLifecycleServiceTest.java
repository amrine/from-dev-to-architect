package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamLifecycleServiceTest {

    private static final Long TEAM_ID = 42L;
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B900";
    private static final String TEAM_REFERENCE = "TEM-2026-1309-00000ZA7B901";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-1309-00000ZA7B902";
    private static final String MANAGER_REFERENCE = "USR-2026-1309-00000ZA7B903";

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ReferenceFactory referenceFactory;

    @Mock
    private TeamOrganizationAvailabilityValidator organizationAvailabilityValidator;

    @Mock
    private TeamResponsibleUsersValidator responsibleUsersValidator;

    @InjectMocks
    private TeamLifecycleService service;

    @Test
    void createsAndPersistsAnActiveTeamAfterValidatingItsDependenciesInOrder() {
        when(referenceFactory.generate("TEM")).thenReturn(TEAM_REFERENCE);
        when(teamRepository.create(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

        Team result = service.create(tenantContext(), validCommand());

        InOrder orderedInteractions =
                inOrder(organizationAvailabilityValidator, responsibleUsersValidator, referenceFactory, teamRepository);
        orderedInteractions.verify(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);
        orderedInteractions
                .verify(responsibleUsersValidator)
                .validateOperationalResponsibleUsers(
                        ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);
        orderedInteractions.verify(referenceFactory).generate("TEM");
        orderedInteractions.verify(teamRepository).create(teamCaptor.capture());

        Team createdTeam = teamCaptor.getValue();
        assertSame(createdTeam, result);
        assertEquals(TEAM_REFERENCE, createdTeam.getReference());
        assertEquals(ORGANIZATION_REFERENCE, createdTeam.getOrganizationReference());
        assertEquals("Platform", createdTeam.getName());
        assertEquals(ADMINISTRATOR_REFERENCE, createdTeam.getAdminReference());
        assertEquals(MANAGER_REFERENCE, createdTeam.getManagerReference());
        assertEquals(TeamStatus.ACTIVE, createdTeam.getStatus());
    }

    @Test
    void returnsThePersistedTeamWithItsInternalIdentifier() {
        when(referenceFactory.generate("TEM")).thenReturn(TEAM_REFERENCE);
        Team persistedTeam = activeTeam();
        when(teamRepository.create(any(Team.class))).thenReturn(persistedTeam);

        Team result = service.create(tenantContext(), validCommand());

        assertSame(persistedTeam, result);
        assertEquals(TEAM_ID, result.getId());
    }

    @Test
    void doesNotWriteWhenOrganizationValidationFails() {
        TeamException validationFailure =
                new TeamException(TeamErrorCode.ORGANIZATION_UNAVAILABLE, "Team organization is not available");
        doThrow(validationFailure).when(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.create(tenantContext(), validCommand()));

        assertSame(validationFailure, exception);
        verifyNoInteractions(responsibleUsersValidator, referenceFactory, teamRepository);
    }

    @Test
    void doesNotWriteWhenAResponsibleUserIsRejected() {
        TeamException validationFailure =
                new TeamException(TeamErrorCode.MANAGER_NOT_AVAILABLE, "Team manager is not available");
        doThrow(validationFailure)
                .when(responsibleUsersValidator)
                .validateOperationalResponsibleUsers(
                        ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.create(tenantContext(), validCommand()));

        assertSame(validationFailure, exception);
        verifyNoInteractions(referenceFactory, teamRepository);
    }

    @Test
    void doesNotWriteWhenReferenceGenerationFails() {
        IllegalStateException generationFailure = new IllegalStateException("Reference generation failed");
        when(referenceFactory.generate("TEM")).thenThrow(generationFailure);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> service.create(tenantContext(), validCommand()));

        assertSame(generationFailure, exception);
        verifyNoInteractions(teamRepository);
    }

    @Test
    void doesNotWriteWhenTeamDomainValidationFails() {
        when(referenceFactory.generate("TEM")).thenReturn(TEAM_REFERENCE);

        TeamException exception = assertThrows(
                TeamException.class,
                () -> service.create(
                        tenantContext(), new CreateTeamCommand(" ", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE)));

        assertEquals(TeamErrorCode.INVALID_NAME, exception.getErrorCode());
        verify(teamRepository, never()).create(any(Team.class));
    }

    @Test
    void suspendsAnExistingTeamInTheCurrentTenant() {
        Team team = activeTeam();
        givenTeam(team);
        when(teamRepository.update(team)).thenReturn(team);

        Team result = service.suspend(tenantContext(), TEAM_REFERENCE);

        assertSame(team, result);
        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
        verify(teamRepository).findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE);
        verify(teamRepository).update(team);
    }

    @Test
    void reactivatesAnExistingTeamAfterRevalidatingItsDependencies() {
        Team team = suspendedTeam();
        givenTeam(team);
        when(teamRepository.update(team)).thenReturn(team);

        Team result = service.reactivate(tenantContext(), TEAM_REFERENCE);

        assertSame(team, result);
        assertEquals(TeamStatus.ACTIVE, team.getStatus());
        InOrder orderedInteractions =
                inOrder(teamRepository, organizationAvailabilityValidator, responsibleUsersValidator);
        orderedInteractions.verify(teamRepository).findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE);
        orderedInteractions.verify(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);
        orderedInteractions
                .verify(responsibleUsersValidator)
                .validateOperationalResponsibleUsers(
                        ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);
        orderedInteractions.verify(teamRepository).update(team);
    }

    @Test
    void rejectsReactivationBeforeCallingDirectoriesWhenTheTeamIsNotSuspended() {
        Team team = activeTeam();
        givenTeam(team);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.reactivate(tenantContext(), TEAM_REFERENCE));

        assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verifyNoInteractions(organizationAvailabilityValidator, responsibleUsersValidator);
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void doesNotWriteWhenTheOrganizationIsUnavailableDuringReactivation() {
        Team team = suspendedTeam();
        givenTeam(team);
        TeamException validationFailure =
                new TeamException(TeamErrorCode.ORGANIZATION_UNAVAILABLE, "Team organization is not available");
        doThrow(validationFailure).when(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.reactivate(tenantContext(), TEAM_REFERENCE));

        assertEquals(TeamErrorCode.ORGANIZATION_UNAVAILABLE, exception.getErrorCode());
        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
        verifyNoInteractions(responsibleUsersValidator);
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void doesNotWriteWhenAResponsibleUserIsRejectedDuringReactivation() {
        Team team = suspendedTeam();
        givenTeam(team);
        TeamException validationFailure =
                new TeamException(TeamErrorCode.MANAGER_NOT_AVAILABLE, "Team manager is not available");
        doThrow(validationFailure)
                .when(responsibleUsersValidator)
                .validateOperationalResponsibleUsers(
                        ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.reactivate(tenantContext(), TEAM_REFERENCE));

        assertSame(validationFailure, exception);
        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
        verify(teamRepository, never()).update(any(Team.class));
    }

    @Test
    void archivesAnExistingTeamInTheCurrentTenant() {
        Team team = activeTeam();
        givenTeam(team);
        when(teamRepository.update(team)).thenReturn(team);

        Team result = service.archive(tenantContext(), TEAM_REFERENCE);

        assertSame(team, result);
        assertEquals(TeamStatus.ARCHIVED, team.getStatus());
        assertEquals(ADMINISTRATOR_REFERENCE, team.getAdminReference());
        assertEquals(MANAGER_REFERENCE, team.getManagerReference());
        verify(teamRepository).update(team);
    }

    @Test
    void doesNotWriteWhenTheTeamIsMissingFromTheCurrentTenant() {
        when(teamRepository.findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.empty());

        TeamException exception =
                assertThrows(TeamException.class, () -> service.suspend(tenantContext(), TEAM_REFERENCE));

        assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(teamRepository, never()).update(any(Team.class));
    }

    private TenantContext tenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private CreateTeamCommand validCommand() {
        return new CreateTeamCommand("Platform", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);
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

    private void givenTeam(Team team) {
        when(teamRepository.findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.of(team));
    }
}
