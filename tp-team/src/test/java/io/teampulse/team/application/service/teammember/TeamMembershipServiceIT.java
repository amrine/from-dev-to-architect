package io.teampulse.team.application.service.teammember;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.team.AbstractTeamJpaIT;
import io.teampulse.team.application.port.in.teammember.TeamMemberCommand;
import io.teampulse.team.application.port.in.teammember.TeamMembershipUseCase;
import io.teampulse.team.application.port.out.team.TeamMemberRepository;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.error.TeamMemberErrorCode;
import io.teampulse.team.domain.team.error.TeamMemberException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.EnumSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamMembershipServiceIT extends AbstractTeamJpaIT {

    private static final String ORGANIZATION_A = "ORG-2026-0916-00000ZA7B900";
    private static final String ORGANIZATION_B = "ORG-2026-0916-00000ZA7B901";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";
    private static final String MEMBER_REFERENCE = "USR-2026-0916-00000ZA7B902";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-16T08:30:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-09-01T09:00:00Z");

    @Inject
    private TeamMembershipUseCase teamMembership;

    @Inject
    private TeamRepository teamRepository;

    @Inject
    private TeamMemberRepository teamMemberRepository;

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
    class AddMember {

        @Test
        void addsAnAvailableUserAndCommitsTheActiveMembership() {
            Team team = givenTeam(TeamStatus.ACTIVE);

            TeamMember member = teamMembership.addMember(tenantContext(ORGANIZATION_A), command());
            TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();
            TeamMemberEntity storedMember = storedMember(team);

            assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
            assertEquals(FIXED_INSTANT, member.getStartedAt());
            assertEquals(TeamMemberStatus.ACTIVE, storedMember.getStatus());
            assertEquals(FIXED_INSTANT, storedMember.getStartedAt());
            assertEquals(1L, jpaTeamMemberRepository.count());
            verify(organizationDirectory).check(ORGANIZATION_A);
            verify(userDirectory).check(ORGANIZATION_A, MEMBER_REFERENCE);
            assertCommittedWriteTransaction(observation, "TeamMembershipService.addMember");
        }

        @Test
        void rejectsAnInvalidCommandBeforeCallingDirectoriesOrPersisting() {
            assertThrows(
                    ConstraintViolationException.class,
                    () -> teamMembership.addMember(
                            tenantContext(ORGANIZATION_A), new TeamMemberCommand(" ", MEMBER_REFERENCE)));

            assertEquals(0L, jpaTeamMemberRepository.count());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @Test
        void rejectsAnUnavailableUserWithoutPersisting() {
            givenTeam(TeamStatus.ACTIVE);
            when(userDirectory.check(ORGANIZATION_A, MEMBER_REFERENCE)).thenReturn(UserAvailability.UNAVAILABLE);

            TeamMemberException exception = assertThrows(
                    TeamMemberException.class,
                    () -> teamMembership.addMember(tenantContext(ORGANIZATION_A), command()));

            assertEquals(TeamMemberErrorCode.USER_NOT_AVAILABLE, exception.getErrorCode());
            assertEquals(0L, jpaTeamMemberRepository.count());
            verify(organizationDirectory).check(ORGANIZATION_A);
            verify(userDirectory).check(ORGANIZATION_A, MEMBER_REFERENCE);
        }

        @Test
        void isolatesTheTeamFromAnotherTenant() {
            givenTeam(TeamStatus.ACTIVE);

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamMembership.addMember(tenantContext(ORGANIZATION_B), command()));

            assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
            assertEquals(0L, jpaTeamMemberRepository.count());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }
    }

    @Nested
    class InviteMember {

        @Test
        void createsAndPersistsAnInvitationForAPendingUser() {
            Team team = givenTeam(TeamStatus.ACTIVE);
            when(userDirectory.check(ORGANIZATION_A, MEMBER_REFERENCE)).thenReturn(UserAvailability.PENDING);

            TeamMember member = teamMembership.inviteMember(tenantContext(ORGANIZATION_A), command());
            TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();
            TeamMemberEntity storedMember = storedMember(team);

            assertEquals(TeamMemberStatus.INVITED, member.getStatus());
            assertNull(member.getStartedAt());
            assertNull(member.getEndedAt());
            assertEquals(TeamMemberStatus.INVITED, storedMember.getStatus());
            assertNull(storedMember.getStartedAt());
            assertNull(storedMember.getEndedAt());
            verify(organizationDirectory).check(ORGANIZATION_A);
            verify(userDirectory).check(ORGANIZATION_A, MEMBER_REFERENCE);
            assertCommittedWriteTransaction(observation, "TeamMembershipService.inviteMember");
        }

        @Test
        void rejectsAnInvitationWhenTheUserAlreadyHasACurrentMembership() {
            Team team = givenTeam(TeamStatus.ACTIVE);
            persistMember(team, TeamMemberStatus.INVITED);

            TeamMemberException exception = assertThrows(
                    TeamMemberException.class,
                    () -> teamMembership.inviteMember(tenantContext(ORGANIZATION_A), command()));

            assertEquals(TeamMemberErrorCode.ALREADY_EXISTS, exception.getErrorCode());
            assertEquals(1L, jpaTeamMemberRepository.count());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        @ParameterizedTest
        @EnumSource(
                value = TeamStatus.class,
                names = {"SUSPENDED", "ARCHIVED"})
        void rejectsInvitingAMemberWhenTheTeamIsNotActive(TeamStatus status) {
            givenTeam(status);

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamMembership.inviteMember(tenantContext(ORGANIZATION_A), command()));

            assertEquals(TeamErrorCode.TEAM_UNAVAILABLE, exception.getErrorCode());
            assertEquals(0L, jpaTeamMemberRepository.count());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }
    }

    @Nested
    class ActivateMember {

        @Test
        void activatesAnInvitationAndPersistsItsStartTime() {
            Team team = givenTeam(TeamStatus.ACTIVE);
            persistMember(team, TeamMemberStatus.INVITED);

            TeamMember member = teamMembership.activateMember(tenantContext(ORGANIZATION_A), command());
            TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();
            TeamMemberEntity storedMember = storedMember(team);

            assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
            assertEquals(FIXED_INSTANT, member.getStartedAt());
            assertEquals(TeamMemberStatus.ACTIVE, storedMember.getStatus());
            assertEquals(FIXED_INSTANT, storedMember.getStartedAt());
            verify(organizationDirectory).check(ORGANIZATION_A);
            verify(userDirectory).check(ORGANIZATION_A, MEMBER_REFERENCE);
            assertCommittedWriteTransaction(observation, "TeamMembershipService.activateMember");
        }

        @Test
        void doesNotCheckDirectoriesOrPersistWhenTheCurrentMembershipIsMissing() {
            givenTeam(TeamStatus.ACTIVE);

            TeamMemberException exception = assertThrows(
                    TeamMemberException.class,
                    () -> teamMembership.activateMember(tenantContext(ORGANIZATION_A), command()));

            assertEquals(TeamMemberErrorCode.NOT_FOUND, exception.getErrorCode());
            assertEquals(0L, jpaTeamMemberRepository.count());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }
    }

    @Nested
    class ReactivateMember {

        @Test
        void reactivatesASuspendedMembershipWithoutChangingItsOriginalStartTime() {
            Team team = givenTeam(TeamStatus.ACTIVE);
            persistMember(team, TeamMemberStatus.SUSPENDED);

            TeamMember member = teamMembership.reactivateMember(tenantContext(ORGANIZATION_A), command());
            TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();
            TeamMemberEntity storedMember = storedMember(team);

            assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
            assertEquals(STARTED_AT, member.getStartedAt());
            assertEquals(TeamMemberStatus.ACTIVE, storedMember.getStatus());
            assertEquals(STARTED_AT, storedMember.getStartedAt());
            verify(organizationDirectory).check(ORGANIZATION_A);
            verify(userDirectory).check(ORGANIZATION_A, MEMBER_REFERENCE);
            assertCommittedWriteTransaction(observation, "TeamMembershipService.reactivateMember");
        }

        @Test
        void rejectsReactivationWhenTheMemberIsNotSuspended() {
            Team team = givenTeam(TeamStatus.ACTIVE);
            persistMember(team, TeamMemberStatus.ACTIVE);

            TeamMemberException exception = assertThrows(
                    TeamMemberException.class,
                    () -> teamMembership.reactivateMember(tenantContext(ORGANIZATION_A), command()));

            assertEquals(TeamMemberErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
            assertEquals(TeamMemberStatus.ACTIVE, storedMember(team).getStatus());
        }
    }

    @Nested
    class SuspendMember {

        @Test
        void suspendsAndPersistsAnActiveMembershipWithoutDirectoryChecks() {
            Team team = givenTeam(TeamStatus.ACTIVE);
            persistMember(team, TeamMemberStatus.ACTIVE);

            TeamMember member = teamMembership.suspendMember(tenantContext(ORGANIZATION_A), command());
            TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();

            assertEquals(TeamMemberStatus.SUSPENDED, member.getStatus());
            assertEquals(TeamMemberStatus.SUSPENDED, storedMember(team).getStatus());
            verifyNoInteractions(organizationDirectory, userDirectory);
            assertCommittedWriteTransaction(observation, "TeamMembershipService.suspendMember");
        }

        @Test
        void rejectsSuspensionWhenTheTeamIsArchived() {
            givenTeam(TeamStatus.ARCHIVED);

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamMembership.suspendMember(tenantContext(ORGANIZATION_A), command()));

            assertEquals(TeamErrorCode.TEAM_UNAVAILABLE, exception.getErrorCode());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }
    }

    @Nested
    class RemoveMember {

        @ParameterizedTest
        @EnumSource(
                value = TeamMemberStatus.class,
                names = {"INVITED", "ACTIVE", "SUSPENDED"})
        void removesCurrentMembershipsAndPersistsTheirEndTime(TeamMemberStatus initialStatus) {
            Team team = givenTeam(TeamStatus.ACTIVE);
            persistMember(team, initialStatus);

            TeamMember member = teamMembership.removeMember(tenantContext(ORGANIZATION_A), command());
            TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();
            TeamMemberEntity storedMember = storedMember(team);

            assertEquals(TeamMemberStatus.REMOVED, member.getStatus());
            assertEquals(FIXED_INSTANT, member.getEndedAt());
            assertEquals(TeamMemberStatus.REMOVED, storedMember.getStatus());
            assertEquals(FIXED_INSTANT, storedMember.getEndedAt());
            if (initialStatus == TeamMemberStatus.INVITED) {
                assertNull(member.getStartedAt());
                assertNull(storedMember.getStartedAt());
            } else {
                assertEquals(STARTED_AT, member.getStartedAt());
                assertEquals(STARTED_AT, storedMember.getStartedAt());
            }
            verifyNoInteractions(organizationDirectory, userDirectory);
            assertCommittedWriteTransaction(observation, "TeamMembershipService.removeMember");
        }

        @Test
        void allowsRemovingAMemberWhenTheTeamIsSuspended() {
            Team team = givenTeam(TeamStatus.SUSPENDED);
            persistMember(team, TeamMemberStatus.ACTIVE);

            TeamMember member = teamMembership.removeMember(tenantContext(ORGANIZATION_A), command());

            assertEquals(TeamMemberStatus.REMOVED, member.getStatus());
            assertEquals(TeamMemberStatus.REMOVED, storedMember(team).getStatus());
        }

        @Test
        void rejectsRemovalWhenTheTeamIsArchived() {
            givenTeam(TeamStatus.ARCHIVED);

            TeamException exception = assertThrows(
                    TeamException.class, () -> teamMembership.removeMember(tenantContext(ORGANIZATION_A), command()));

            assertEquals(TeamErrorCode.TEAM_UNAVAILABLE, exception.getErrorCode());
            verifyNoInteractions(organizationDirectory, userDirectory);
        }
    }

    @Nested
    class TenantContextValidation {

        @ParameterizedTest
        @MethodSource("membershipOperations")
        void rejectsANullTenantContextForEveryOperation(String operation) {
            assertThrows(ConstraintViolationException.class, () -> {
                switch (operation) {
                    case "add" -> teamMembership.addMember(null, command());
                    case "invite" -> teamMembership.inviteMember(null, command());
                    case "activate" -> teamMembership.activateMember(null, command());
                    case "suspend" -> teamMembership.suspendMember(null, command());
                    case "reactivate" -> teamMembership.reactivateMember(null, command());
                    case "remove" -> teamMembership.removeMember(null, command());
                    default -> throw new IllegalArgumentException("Unknown operation: " + operation);
                }
            });

            verifyNoInteractions(organizationDirectory, userDirectory);
        }

        private static Stream<String> membershipOperations() {
            return Stream.of("add", "invite", "activate", "suspend", "reactivate", "remove");
        }
    }

    private Team givenTeam(TeamStatus status) {
        Team team = Team.create(
                TEAM_REFERENCE, ORGANIZATION_A, "TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE);
        switch (status) {
            case ACTIVE -> {}
            case SUSPENDED -> team.suspend();
            case ARCHIVED -> team.archive();
        }
        return teamRepository.create(team);
    }

    private TeamMember persistMember(Team team, TeamMemberStatus status) {
        TeamMember member = switch (status) {
            case INVITED -> TeamMember.invite(ORGANIZATION_A, team.getId(), MEMBER_REFERENCE);
            case ACTIVE -> TeamMember.add(ORGANIZATION_A, team.getId(), MEMBER_REFERENCE, STARTED_AT);
            case SUSPENDED -> {
                TeamMember active = TeamMember.add(ORGANIZATION_A, team.getId(), MEMBER_REFERENCE, STARTED_AT);
                active.suspend();
                yield active;
            }
            case REMOVED -> {
                TeamMember active = TeamMember.add(ORGANIZATION_A, team.getId(), MEMBER_REFERENCE, STARTED_AT);
                active.remove(FIXED_INSTANT);
                yield active;
            }
        };
        return teamMemberRepository.create(member);
    }

    private TeamMemberEntity storedMember(Team team) {
        return jpaTeamMemberRepository
                .findByOrganizationReferenceAndTeamIdAndUserReferenceAndStatusIn(
                        ORGANIZATION_A, team.getId(), MEMBER_REFERENCE, EnumSet.allOf(TeamMemberStatus.class))
                .orElseThrow();
    }

    private static TenantContext tenantContext(String organizationReference) {
        return new TenantContext(organizationReference);
    }

    private static TeamMemberCommand command() {
        return new TeamMemberCommand(TEAM_REFERENCE, MEMBER_REFERENCE);
    }

    private void assertCommittedWriteTransaction(
            TransactionManagerProbe.TransactionObservation observation, String transactionNameSuffix) {
        assertTrue(observation.name().endsWith(transactionNameSuffix));
        assertFalse(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }
}
