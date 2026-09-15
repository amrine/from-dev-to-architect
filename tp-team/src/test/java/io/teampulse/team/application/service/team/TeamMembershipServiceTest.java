package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.application.port.in.team.TeamMemberCommand;
import io.teampulse.team.application.port.out.team.TeamMemberRepository;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.domain.team.model.TeamStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamMembershipServiceTest {

    private static final Long TEAM_ID = 42L;
    private static final Long MEMBER_ID = 84L;
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B900";
    private static final String TEAM_REFERENCE = "TEM-2026-1309-00000ZA7B901";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-1309-00000ZA7B902";
    private static final String MANAGER_REFERENCE = "USR-2026-1309-00000ZA7B903";
    private static final String MEMBER_REFERENCE = "USR-2026-1309-00000ZA7B904";
    private static final Instant NOW = Instant.parse("2026-09-16T10:15:30Z");
    private static final Instant STARTED_AT = Instant.parse("2026-09-01T09:00:00Z");

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamOrganizationAvailabilityValidator organizationAvailabilityValidator;

    @Mock
    private TeamMemberUserAvailabilityValidator memberUserAvailabilityValidator;

    @Spy
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @InjectMocks
    private TeamMembershipService service;

    @Test
    void addsAnAvailableUserAsAnActiveMember() {
        Team team = activeTeam();
        givenTeam(team);
        givenNoCurrentMembership(team);
        when(teamMemberRepository.create(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);

        TeamMember result = service.addMember(tenantContext(), command());

        verify(teamMemberRepository).create(memberCaptor.capture());
        TeamMember createdMember = memberCaptor.getValue();
        assertSame(createdMember, result);
        assertEquals(TeamMemberStatus.ACTIVE, createdMember.getStatus());
        assertEquals(NOW, createdMember.getStartedAt());
        assertNull(createdMember.getEndedAt());
        verify(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);
        verify(memberUserAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE, MEMBER_REFERENCE);
    }

    @Test
    void invitesAPendingUser() {
        Team team = activeTeam();
        givenTeam(team);
        givenNoCurrentMembership(team);
        when(teamMemberRepository.create(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamMember result = service.inviteMember(tenantContext(), command());

        assertEquals(TeamMemberStatus.INVITED, result.getStatus());
        assertNull(result.getStartedAt());
        assertNull(result.getEndedAt());
        verify(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);
        verify(memberUserAvailabilityValidator).validateAvailableOrPending(ORGANIZATION_REFERENCE, MEMBER_REFERENCE);
    }

    @Test
    void activatesAnInvitedMemberAfterRevalidatingItsDependencies() {
        Team team = activeTeam();
        TeamMember member = invitedMember();
        givenTeam(team);
        givenCurrentMembership(team, member);
        when(teamMemberRepository.update(member)).thenReturn(member);

        TeamMember result = service.activateMember(tenantContext(), command());

        assertSame(member, result);
        assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
        assertEquals(NOW, member.getStartedAt());
        assertNull(member.getEndedAt());
        verify(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);
        verify(memberUserAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE, MEMBER_REFERENCE);
        verify(teamMemberRepository).update(member);
    }

    @Test
    void reactivatesASuspendedMemberWithoutChangingItsInitialStartDate() {
        Team team = activeTeam();
        TeamMember member = suspendedMember();
        givenTeam(team);
        givenCurrentMembership(team, member);
        when(teamMemberRepository.update(member)).thenReturn(member);

        TeamMember result = service.reactivateMember(tenantContext(), command());

        assertSame(member, result);
        assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
        assertEquals(STARTED_AT, member.getStartedAt());
        assertNull(member.getEndedAt());
        verify(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);
        verify(memberUserAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE, MEMBER_REFERENCE);
    }

    @Test
    void suspendsAnActiveMemberWithoutCheckingOrganizationOrUserAvailability() {
        Team team = activeTeam();
        TeamMember member = activeMember();
        givenTeam(team);
        givenCurrentMembership(team, member);
        when(teamMemberRepository.update(member)).thenReturn(member);

        TeamMember result = service.suspendMember(tenantContext(), command());

        assertSame(member, result);
        assertEquals(TeamMemberStatus.SUSPENDED, member.getStatus());
        verifyNoInteractions(organizationAvailabilityValidator, memberUserAvailabilityValidator);
    }

    @Test
    void removesAnInvitationWithoutCheckingOrganizationOrUserAvailability() {
        Team team = activeTeam();
        TeamMember member = invitedMember();
        givenTeam(team);
        givenCurrentMembership(team, member);
        when(teamMemberRepository.update(member)).thenReturn(member);

        TeamMember result = service.removeMember(tenantContext(), command());

        assertSame(member, result);
        assertEquals(TeamMemberStatus.REMOVED, member.getStatus());
        assertNull(member.getStartedAt());
        assertEquals(NOW, member.getEndedAt());
        verifyNoInteractions(organizationAvailabilityValidator, memberUserAvailabilityValidator);
    }

    @Test
    void allowsMemberRemovalWhenTheTeamIsSuspended() {
        Team team = suspendedTeam();
        TeamMember member = activeMember();
        givenTeam(team);
        givenCurrentMembership(team, member);
        when(teamMemberRepository.update(member)).thenReturn(member);

        TeamMember result = service.removeMember(tenantContext(), command());

        assertSame(member, result);
        assertEquals(TeamMemberStatus.REMOVED, member.getStatus());
        verifyNoInteractions(organizationAvailabilityValidator, memberUserAvailabilityValidator);
    }

    @ParameterizedTest
    @EnumSource(
            value = TeamStatus.class,
            names = {"SUSPENDED", "ARCHIVED"})
    void rejectsOperationsRequiringAnActiveTeam(TeamStatus status) {
        Team team = team(status);
        givenTeam(team);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.addMember(tenantContext(), command()));

        assertEquals(TeamErrorCode.TEAM_UNAVAILABLE, exception.getErrorCode());
        verifyNoInteractions(teamMemberRepository, organizationAvailabilityValidator, memberUserAvailabilityValidator);
    }

    @Test
    void rejectsMemberRemovalWhenTheTeamIsArchived() {
        Team team = archivedTeam();
        givenTeam(team);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.removeMember(tenantContext(), command()));

        assertEquals(TeamErrorCode.TEAM_UNAVAILABLE, exception.getErrorCode());
        verifyNoInteractions(teamMemberRepository, organizationAvailabilityValidator, memberUserAvailabilityValidator);
    }

    @Test
    void rejectsAnInvitationWhenTheUserAlreadyHasACurrentMembership() {
        Team team = activeTeam();
        givenTeam(team);
        givenCurrentMembership(team, invitedMember());

        TeamException exception =
                assertThrows(TeamException.class, () -> service.inviteMember(tenantContext(), command()));

        assertEquals(TeamErrorCode.MEMBER_ALREADY_EXISTS, exception.getErrorCode());
        verifyNoInteractions(organizationAvailabilityValidator, memberUserAvailabilityValidator);
        verify(teamMemberRepository, never()).create(any(TeamMember.class));
    }

    @Test
    void createsANewInvitationWhenNoCurrentMembershipExists() {
        Team team = activeTeam();
        givenTeam(team);
        givenNoCurrentMembership(team);
        when(teamMemberRepository.create(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamMember result = service.inviteMember(tenantContext(), command());

        assertNull(result.getId());
        assertEquals(TeamMemberStatus.INVITED, result.getStatus());
        assertNull(result.getStartedAt());
        assertNull(result.getEndedAt());
        verify(teamMemberRepository, never()).update(any(TeamMember.class));
    }

    @Test
    void doesNotWriteWhenOrganizationValidationFails() {
        Team team = activeTeam();
        givenTeam(team);
        givenNoCurrentMembership(team);
        TeamException validationFailure =
                new TeamException(TeamErrorCode.ORGANIZATION_UNAVAILABLE, "Team organization is not available");
        doThrow(validationFailure).when(organizationAvailabilityValidator).validateAvailable(ORGANIZATION_REFERENCE);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.addMember(tenantContext(), command()));

        assertSame(validationFailure, exception);
        verifyNoInteractions(memberUserAvailabilityValidator);
        verify(teamMemberRepository, never()).create(any(TeamMember.class));
    }

    @Test
    void doesNotWriteWhenMemberUserValidationFails() {
        Team team = activeTeam();
        givenTeam(team);
        givenNoCurrentMembership(team);
        TeamException validationFailure =
                new TeamException(TeamErrorCode.MEMBER_USER_NOT_AVAILABLE, "Team member user is not available");
        doThrow(validationFailure)
                .when(memberUserAvailabilityValidator)
                .validateAvailable(ORGANIZATION_REFERENCE, MEMBER_REFERENCE);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.addMember(tenantContext(), command()));

        assertSame(validationFailure, exception);
        verify(teamMemberRepository, never()).create(any(TeamMember.class));
    }

    @Test
    void doesNotCallDirectoriesWhenTheCurrentMembershipIsMissing() {
        Team team = activeTeam();
        givenTeam(team);
        givenNoCurrentMembership(team);

        TeamException exception =
                assertThrows(TeamException.class, () -> service.activateMember(tenantContext(), command()));

        assertEquals(TeamErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(organizationAvailabilityValidator, memberUserAvailabilityValidator);
        verify(teamMemberRepository, never()).update(any(TeamMember.class));
    }

    private TenantContext tenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private TeamMemberCommand command() {
        return new TeamMemberCommand(TEAM_REFERENCE, MEMBER_REFERENCE);
    }

    private Team activeTeam() {
        return team(TeamStatus.ACTIVE);
    }

    private Team suspendedTeam() {
        return team(TeamStatus.SUSPENDED);
    }

    private Team archivedTeam() {
        return team(TeamStatus.ARCHIVED);
    }

    private Team team(TeamStatus status) {
        return Team.restore(
                TEAM_ID,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "Platform",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                status);
    }

    private TeamMember invitedMember() {
        return TeamMember.restore(
                MEMBER_ID, ORGANIZATION_REFERENCE, TEAM_ID, MEMBER_REFERENCE, TeamMemberStatus.INVITED, null, null);
    }

    private TeamMember activeMember() {
        return TeamMember.restore(
                MEMBER_ID,
                ORGANIZATION_REFERENCE,
                TEAM_ID,
                MEMBER_REFERENCE,
                TeamMemberStatus.ACTIVE,
                STARTED_AT,
                null);
    }

    private TeamMember suspendedMember() {
        return TeamMember.restore(
                MEMBER_ID,
                ORGANIZATION_REFERENCE,
                TEAM_ID,
                MEMBER_REFERENCE,
                TeamMemberStatus.SUSPENDED,
                STARTED_AT,
                null);
    }

    private void givenTeam(Team team) {
        when(teamRepository.findByReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.of(team));
    }

    private void givenCurrentMembership(Team team, TeamMember member) {
        when(teamMemberRepository.findCurrent(ORGANIZATION_REFERENCE, team.getId(), MEMBER_REFERENCE))
                .thenReturn(Optional.of(member));
    }

    private void givenNoCurrentMembership(Team team) {
        when(teamMemberRepository.findCurrent(ORGANIZATION_REFERENCE, team.getId(), MEMBER_REFERENCE))
                .thenReturn(Optional.empty());
    }
}
