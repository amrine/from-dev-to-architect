package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.team.AbstractTeamJpaIT;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TeamLifecycleServiceIT extends AbstractTeamJpaIT {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B900";
    private static final String OTHER_ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B901";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";
    private static final String UNKNOWN_TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";

    @Inject
    private TeamLifecycleUseCase teamLifecycle;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @MockitoBean
    private OrganizationDirectory organizationDirectory;

    @MockitoBean
    private UserDirectory userDirectory;

    @BeforeEach
    void cleanDatabaseAndConfigureDirectories() {
        inTransactionTemplate(() -> {
            jpaTeamMemberRepository.deleteAllInBatch();
            jpaTeamRepository.deleteAllInBatch();
        });
        when(organizationDirectory.check(anyString())).thenReturn(OrganizationAvailability.AVAILABLE);
        when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.AVAILABLE);
    }

    @Nested
    class CreateTeam {

        @Test
        void createsAndPersistsAnActiveTeamInsideAnActiveReadWriteTransaction() {
            Team team = teamLifecycle.create(tenantContext(), command(" TeamPulse Engineering "));
            TeamEntity storedTeam = storedTeam(team);

            assertTrue(ReferenceFormat.matches(team.getReference(), "TEM"));
            assertEquals("TeamPulse Engineering", team.getName());
            assertEquals(TeamStatus.ACTIVE, team.getStatus());
            assertEquals(team.getId(), storedTeam.getId());
            assertEquals(ORGANIZATION_REFERENCE, storedTeam.getOrganizationReference());
            assertEquals(ADMIN_REFERENCE, storedTeam.getAdminReference());
            assertEquals(MANAGER_REFERENCE, storedTeam.getManagerReference());
            verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
            verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMIN_REFERENCE);
            verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
            assertCommittedWriteTransaction("TeamLifecycleService.create");
        }

        @Test
        void checksTheDirectoryOnlyOnceWhenAdministratorAndManagerAreTheSameUser() {
            teamLifecycle.create(tenantContext(), command("Platform", ADMIN_REFERENCE, ADMIN_REFERENCE));

            verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMIN_REFERENCE);
            verifyNoMoreInteractions(userDirectory);
            assertEquals(1L, jpaTeamRepository.count());
        }

        @Test
        void rejectsANullTenantContextBeforeCallingDirectoriesOrPersisting() {
            assertThrows(ConstraintViolationException.class, () -> teamLifecycle.create(null, command("Platform")));

            assertNoTeamsPersisted();
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsANullCommandBeforeCallingDirectoriesOrPersisting() {
            assertThrows(ConstraintViolationException.class, () -> teamLifecycle.create(tenantContext(), null));

            assertNoTeamsPersisted();
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @ParameterizedTest
        @MethodSource("invalidCommands")
        void rejectsCommandsWithMissingRequiredFields(CreateTeamCommand command) {
            assertThrows(ConstraintViolationException.class, () -> teamLifecycle.create(tenantContext(), command));

            assertNoTeamsPersisted();
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsANameLongerThanTheDomainLimitWithoutPersisting() {
            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.create(tenantContext(), command("x".repeat(201))));

            assertEquals(TeamErrorCode.INVALID_NAME, exception.getErrorCode());
            assertNoTeamsPersisted();
        }

        @ParameterizedTest
        @CsvSource({"UNAVAILABLE, ORGANIZATION_UNAVAILABLE", "NOT_FOUND, ORGANIZATION_NOT_FOUND"})
        void rejectsAnOrganizationThatCannotOwnAnOperationalTeam(
                OrganizationAvailability availability, TeamErrorCode expectedError) {
            when(organizationDirectory.check(ORGANIZATION_REFERENCE)).thenReturn(availability);

            TeamException exception =
                    assertThrows(TeamException.class, () -> teamLifecycle.create(tenantContext(), command("Platform")));

            assertEquals(expectedError, exception.getErrorCode());
            assertNoTeamsPersisted();
            verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
            verifyNoInteractions(userDirectory);
        }

        @Test
        void reportsAnOrganizationDirectoryFailureWithoutPersisting() {
            when(organizationDirectory.check(ORGANIZATION_REFERENCE))
                    .thenThrow(new OrganizationDirectoryException(new IllegalStateException("directory down")));

            TeamException exception =
                    assertThrows(TeamException.class, () -> teamLifecycle.create(tenantContext(), command("Platform")));

            assertEquals(TeamErrorCode.ORGANIZATION_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
            assertInstanceOf(OrganizationDirectoryException.class, exception.getCause());
            assertNoTeamsPersisted();
            verifyNoInteractions(userDirectory);
        }

        @ParameterizedTest
        @CsvSource({
            "ADMINISTRATOR, NOT_FOUND, ADMINISTRATOR_NOT_FOUND",
            "ADMINISTRATOR, PENDING, ADMINISTRATOR_NOT_AVAILABLE",
            "ADMINISTRATOR, UNAVAILABLE, ADMINISTRATOR_NOT_AVAILABLE",
            "MANAGER, NOT_FOUND, MANAGER_NOT_FOUND",
            "MANAGER, PENDING, MANAGER_NOT_AVAILABLE",
            "MANAGER, UNAVAILABLE, MANAGER_NOT_AVAILABLE"
        })
        void rejectsAResponsibleUserThatIsNotOperational(
                String role, UserAvailability availability, TeamErrorCode expectedError) {
            String rejectedReference = role.equals("ADMINISTRATOR") ? ADMIN_REFERENCE : MANAGER_REFERENCE;
            when(userDirectory.check(ORGANIZATION_REFERENCE, rejectedReference)).thenReturn(availability);

            TeamException exception =
                    assertThrows(TeamException.class, () -> teamLifecycle.create(tenantContext(), command("Platform")));

            assertEquals(expectedError, exception.getErrorCode());
            assertNoTeamsPersisted();
            verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
            verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMIN_REFERENCE);
            if (role.equals("MANAGER")) {
                verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
            } else {
                verifyNoManagerDirectoryCall();
            }
        }

        @ParameterizedTest
        @CsvSource({"ADMINISTRATOR", "MANAGER"})
        void reportsAUserDirectoryFailureWithoutPersisting(String role) {
            String rejectedReference = role.equals("ADMINISTRATOR") ? ADMIN_REFERENCE : MANAGER_REFERENCE;
            when(userDirectory.check(ORGANIZATION_REFERENCE, rejectedReference))
                    .thenThrow(new UserDirectoryException(new IllegalStateException("directory down")));

            TeamException exception =
                    assertThrows(TeamException.class, () -> teamLifecycle.create(tenantContext(), command("Platform")));

            assertEquals(TeamErrorCode.USER_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
            assertInstanceOf(UserDirectoryException.class, exception.getCause());
            assertNoTeamsPersisted();
            if (role.equals("ADMINISTRATOR")) {
                verifyNoManagerDirectoryCall();
            } else {
                verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
            }
        }

        private static Stream<CreateTeamCommand> invalidCommands() {
            return Stream.of(
                    command(null, ADMIN_REFERENCE, MANAGER_REFERENCE),
                    command(" ", ADMIN_REFERENCE, MANAGER_REFERENCE),
                    command("Platform", null, MANAGER_REFERENCE),
                    command("Platform", " ", MANAGER_REFERENCE),
                    command("Platform", ADMIN_REFERENCE, null),
                    command("Platform", ADMIN_REFERENCE, " "));
        }
    }

    @Nested
    class SuspendTeam {

        @Test
        void suspendsAnActiveTeamAndCommitsTheStatusChange() {
            Team existingTeam = createPersistedTeam();
            clearDirectoryInvocations();

            Team suspendedTeam = teamLifecycle.suspend(tenantContext(), existingTeam.getReference());

            assertEquals(TeamStatus.SUSPENDED, suspendedTeam.getStatus());
            assertEquals(TeamStatus.SUSPENDED, storedTeam(existingTeam).getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
            assertCommittedWriteTransaction("TeamLifecycleService.suspend");
        }

        @ParameterizedTest
        @EnumSource(
                value = TeamStatus.class,
                names = {"SUSPENDED", "ARCHIVED"})
        void rejectsSuspensionFromAStatusThatCannotTransitionToSuspended(TeamStatus initialStatus) {
            Team existingTeam = createPersistedTeam(initialStatus);
            clearDirectoryInvocations();

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.suspend(tenantContext(), existingTeam.getReference()));

            assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
            assertEquals(initialStatus, storedTeam(existingTeam).getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsATeamOutsideTheCurrentTenant() {
            Team existingTeam = createPersistedTeam();
            clearDirectoryInvocations();

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> teamLifecycle.suspend(
                            new TenantContext(OTHER_ORGANIZATION_REFERENCE), existingTeam.getReference()));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            assertEquals(TeamStatus.ACTIVE, storedTeam(existingTeam).getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsAnUnknownTeam() {
            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.suspend(tenantContext(), UNKNOWN_TEAM_REFERENCE));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            assertNoTeamsPersisted();
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsNullTenantContext() {
            assertThrows(ConstraintViolationException.class, () -> teamLifecycle.suspend(null, UNKNOWN_TEAM_REFERENCE));

            assertNoTeamsPersisted();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void rejectsABlankTeamReference(String teamReference) {
            assertThrows(
                    ConstraintViolationException.class, () -> teamLifecycle.suspend(tenantContext(), teamReference));

            assertNoTeamsPersisted();
        }

        @Test
        void suspendsEvenWhenOrganizationAndUsersAreUnavailable() {
            Team existingTeam = createPersistedTeam();
            clearDirectoryInvocations();
            when(organizationDirectory.check(ORGANIZATION_REFERENCE)).thenReturn(OrganizationAvailability.UNAVAILABLE);
            when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.UNAVAILABLE);

            Team suspendedTeam = teamLifecycle.suspend(tenantContext(), existingTeam.getReference());

            assertEquals(TeamStatus.SUSPENDED, suspendedTeam.getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }
    }

    @Nested
    class ReactivateTeam {

        @Test
        void reactivatesAValidSuspendedTeamAndCommitsTheStatusChange() {
            Team existingTeam = createPersistedTeam(TeamStatus.SUSPENDED);
            clearDirectoryInvocations();

            Team reactivatedTeam = teamLifecycle.reactivate(tenantContext(), existingTeam.getReference());

            assertEquals(TeamStatus.ACTIVE, reactivatedTeam.getStatus());
            assertEquals(TeamStatus.ACTIVE, storedTeam(existingTeam).getStatus());
            verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
            verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMIN_REFERENCE);
            verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
            assertCommittedWriteTransaction("TeamLifecycleService.reactivate");
        }

        @ParameterizedTest
        @EnumSource(
                value = TeamStatus.class,
                names = {"ACTIVE", "ARCHIVED"})
        void rejectsReactivationFromAStatusThatCannotTransitionToActive(TeamStatus initialStatus) {
            Team existingTeam = createPersistedTeam(initialStatus);
            clearDirectoryInvocations();

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.reactivate(tenantContext(), existingTeam.getReference()));

            assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
            assertEquals(initialStatus, storedTeam(existingTeam).getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsATeamOutsideTheCurrentTenant() {
            Team existingTeam = createPersistedTeam(TeamStatus.SUSPENDED);
            clearDirectoryInvocations();

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> teamLifecycle.reactivate(
                            new TenantContext(OTHER_ORGANIZATION_REFERENCE), existingTeam.getReference()));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            assertEquals(TeamStatus.SUSPENDED, storedTeam(existingTeam).getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsAnUnknownTeam() {
            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.reactivate(tenantContext(), UNKNOWN_TEAM_REFERENCE));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            assertNoTeamsPersisted();
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsNullTenantContext() {
            assertThrows(
                    ConstraintViolationException.class, () -> teamLifecycle.reactivate(null, UNKNOWN_TEAM_REFERENCE));

            assertNoTeamsPersisted();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void rejectsABlankTeamReference(String teamReference) {
            assertThrows(
                    ConstraintViolationException.class, () -> teamLifecycle.reactivate(tenantContext(), teamReference));

            assertNoTeamsPersisted();
        }

        @ParameterizedTest
        @CsvSource({"UNAVAILABLE, ORGANIZATION_UNAVAILABLE", "NOT_FOUND, ORGANIZATION_NOT_FOUND"})
        void doesNotReactivateWhenTheOrganizationIsNotOperational(
                OrganizationAvailability availability, TeamErrorCode expectedError) {
            Team existingTeam = createPersistedTeam(TeamStatus.SUSPENDED);
            clearDirectoryInvocations();
            when(organizationDirectory.check(ORGANIZATION_REFERENCE)).thenReturn(availability);

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.reactivate(tenantContext(), existingTeam.getReference()));

            assertEquals(expectedError, exception.getErrorCode());
            assertEquals(TeamStatus.SUSPENDED, storedTeam(existingTeam).getStatus());
            verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
            verifyNoInteractions(userDirectory);
        }

        @Test
        void reportsAnOrganizationDirectoryFailureAndLeavesTheTeamSuspended() {
            Team existingTeam = createPersistedTeam(TeamStatus.SUSPENDED);
            clearDirectoryInvocations();
            when(organizationDirectory.check(ORGANIZATION_REFERENCE))
                    .thenThrow(new OrganizationDirectoryException(new IllegalStateException("directory down")));

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.reactivate(tenantContext(), existingTeam.getReference()));

            assertEquals(TeamErrorCode.ORGANIZATION_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
            assertInstanceOf(OrganizationDirectoryException.class, exception.getCause());
            assertEquals(TeamStatus.SUSPENDED, storedTeam(existingTeam).getStatus());
            verifyNoInteractions(userDirectory);
        }

        @ParameterizedTest
        @CsvSource({
            "ADMINISTRATOR, NOT_FOUND, ADMINISTRATOR_NOT_FOUND",
            "ADMINISTRATOR, PENDING, ADMINISTRATOR_NOT_AVAILABLE",
            "ADMINISTRATOR, UNAVAILABLE, ADMINISTRATOR_NOT_AVAILABLE",
            "MANAGER, NOT_FOUND, MANAGER_NOT_FOUND",
            "MANAGER, PENDING, MANAGER_NOT_AVAILABLE",
            "MANAGER, UNAVAILABLE, MANAGER_NOT_AVAILABLE"
        })
        void doesNotReactivateWhenAResponsibleUserIsNotOperational(
                String role, UserAvailability availability, TeamErrorCode expectedError) {
            Team existingTeam = createPersistedTeam(TeamStatus.SUSPENDED);
            clearDirectoryInvocations();
            String rejectedReference = role.equals("ADMINISTRATOR") ? ADMIN_REFERENCE : MANAGER_REFERENCE;
            when(userDirectory.check(ORGANIZATION_REFERENCE, rejectedReference)).thenReturn(availability);

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.reactivate(tenantContext(), existingTeam.getReference()));

            assertEquals(expectedError, exception.getErrorCode());
            assertEquals(TeamStatus.SUSPENDED, storedTeam(existingTeam).getStatus());
            verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
            verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMIN_REFERENCE);
            if (role.equals("MANAGER")) {
                verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
            } else {
                verifyNoManagerDirectoryCall();
            }
        }

        @ParameterizedTest
        @CsvSource({"ADMINISTRATOR", "MANAGER"})
        void reportsAUserDirectoryFailureAndLeavesTheTeamSuspended(String role) {
            Team existingTeam = createPersistedTeam(TeamStatus.SUSPENDED);
            clearDirectoryInvocations();
            String rejectedReference = role.equals("ADMINISTRATOR") ? ADMIN_REFERENCE : MANAGER_REFERENCE;
            when(userDirectory.check(ORGANIZATION_REFERENCE, rejectedReference))
                    .thenThrow(new UserDirectoryException(new IllegalStateException("directory down")));

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.reactivate(tenantContext(), existingTeam.getReference()));

            assertEquals(TeamErrorCode.USER_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
            assertInstanceOf(UserDirectoryException.class, exception.getCause());
            assertEquals(TeamStatus.SUSPENDED, storedTeam(existingTeam).getStatus());
            if (role.equals("ADMINISTRATOR")) {
                verifyNoManagerDirectoryCall();
            } else {
                verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
            }
        }
    }

    @Nested
    class ArchiveTeam {

        @ParameterizedTest
        @EnumSource(
                value = TeamStatus.class,
                names = {"ACTIVE", "SUSPENDED"})
        void archivesAnActiveOrSuspendedTeamAndPreservesItsResponsibleUsers(TeamStatus initialStatus) {
            Team existingTeam = createPersistedTeam(initialStatus);
            clearDirectoryInvocations();

            Team archivedTeam = teamLifecycle.archive(tenantContext(), existingTeam.getReference());
            TeamEntity storedTeam = storedTeam(existingTeam);

            assertEquals(TeamStatus.ARCHIVED, archivedTeam.getStatus());
            assertEquals(TeamStatus.ARCHIVED, storedTeam.getStatus());
            assertEquals(ADMIN_REFERENCE, storedTeam.getAdminReference());
            assertEquals(MANAGER_REFERENCE, storedTeam.getManagerReference());
            verifyNoInteractions(organizationDirectory, userDirectory);
            assertCommittedWriteTransaction("TeamLifecycleService.archive");
        }

        @Test
        void rejectsArchivingAnAlreadyArchivedTeam() {
            Team existingTeam = createPersistedTeam(TeamStatus.ARCHIVED);

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.archive(tenantContext(), existingTeam.getReference()));

            assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
            assertEquals(TeamStatus.ARCHIVED, storedTeam(existingTeam).getStatus());
        }

        @Test
        void rejectsATeamOutsideTheCurrentTenant() {
            Team existingTeam = createPersistedTeam();
            clearDirectoryInvocations();

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> teamLifecycle.archive(
                            new TenantContext(OTHER_ORGANIZATION_REFERENCE), existingTeam.getReference()));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            assertEquals(TeamStatus.ACTIVE, storedTeam(existingTeam).getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsAnUnknownTeam() {
            TeamException exception = assertThrows(
                    TeamException.class, () -> teamLifecycle.archive(tenantContext(), UNKNOWN_TEAM_REFERENCE));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            assertNoTeamsPersisted();
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsNullTenantContext() {
            assertThrows(ConstraintViolationException.class, () -> teamLifecycle.archive(null, UNKNOWN_TEAM_REFERENCE));

            assertNoTeamsPersisted();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void rejectsABlankTeamReference(String teamReference) {
            assertThrows(
                    ConstraintViolationException.class, () -> teamLifecycle.archive(tenantContext(), teamReference));

            assertNoTeamsPersisted();
        }

        @Test
        void archivesEvenWhenOrganizationAndUsersAreUnavailable() {
            Team existingTeam = createPersistedTeam();
            clearDirectoryInvocations();
            when(organizationDirectory.check(ORGANIZATION_REFERENCE)).thenReturn(OrganizationAvailability.UNAVAILABLE);
            when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.UNAVAILABLE);

            Team archivedTeam = teamLifecycle.archive(tenantContext(), existingTeam.getReference());

            assertEquals(TeamStatus.ARCHIVED, archivedTeam.getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }
    }

    private Team createPersistedTeam() {
        return teamLifecycle.create(tenantContext(), command("TeamPulse Engineering"));
    }

    private Team createPersistedTeam(TeamStatus status) {
        Team team = createPersistedTeam();
        return switch (status) {
            case ACTIVE -> team;
            case SUSPENDED -> teamLifecycle.suspend(tenantContext(), team.getReference());
            case ARCHIVED -> teamLifecycle.archive(tenantContext(), team.getReference());
        };
    }

    private TeamEntity storedTeam(Team team) {
        return jpaTeamRepository
                .findByOrganizationReferenceAndReference(ORGANIZATION_REFERENCE, team.getReference())
                .orElseThrow();
    }

    private void assertNoTeamsPersisted() {
        assertEquals(0L, jpaTeamRepository.count());
    }

    private void assertCommittedWriteTransaction(String expectedNameSuffix) {
        TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();
        assertTrue(observation.name().endsWith(expectedNameSuffix));
        assertFalse(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    private void clearDirectoryInvocations() {
        clearInvocations(organizationDirectory, userDirectory);
    }

    private void verifyNoManagerDirectoryCall() {
        verify(userDirectory, never()).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
    }

    private static TenantContext tenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private static CreateTeamCommand command(String name) {
        return command(name, ADMIN_REFERENCE, MANAGER_REFERENCE);
    }

    private static CreateTeamCommand command(String name, String adminReference, String managerReference) {
        return new CreateTeamCommand(name, adminReference, managerReference);
    }
}
