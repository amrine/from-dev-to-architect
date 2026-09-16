package io.teampulse.team.domain.team.model;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamMemberTest {

    private static final Long MEMBER_ID = 24L;
    private static final Long TEAM_ID = 42L;
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B901";
    private static final String USER_REFERENCE = "USR-2026-1309-00000ZA7B902";
    private static final Instant STARTED_AT = Instant.parse("2026-09-14T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-09-14T11:00:00Z");

    @Test
    void createsAnInvitationWithoutPersistenceIdentityOrDates() {
        TeamMember member = inviteMember();

        assertNull(member.getId());
        assertEquals(ORGANIZATION_REFERENCE, member.getOrganizationReference());
        assertEquals(TEAM_ID, member.getTeamId());
        assertEquals(USER_REFERENCE, member.getUserReference());
        assertEquals(TeamMemberStatus.INVITED, member.getStatus());
        assertNull(member.getStartedAt());
        assertNull(member.getEndedAt());
    }

    @Test
    void addsAnActiveMemberWithItsInitialStartDate() {
        TeamMember member = TeamMember.add(ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE, STARTED_AT);

        assertNull(member.getId());
        assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
        assertEquals(STARTED_AT, member.getStartedAt());
        assertNull(member.getEndedAt());
    }

    @Test
    void restoresEveryValidTemporalState() {
        assertRestoredState(TeamMemberStatus.INVITED, null, null);
        assertRestoredState(TeamMemberStatus.ACTIVE, STARTED_AT, null);
        assertRestoredState(TeamMemberStatus.SUSPENDED, STARTED_AT, null);
        assertRestoredState(TeamMemberStatus.REMOVED, null, ENDED_AT);
        assertRestoredState(TeamMemberStatus.REMOVED, STARTED_AT, ENDED_AT);
    }

    @Test
    void activatesAnInvitationAndInitializesItsStartDateOnce() {
        TeamMember member = inviteMember();

        member.activate(STARTED_AT);

        assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
        assertEquals(STARTED_AT, member.getStartedAt());
        assertNull(member.getEndedAt());
    }

    @Test
    void suspendsAndReactivatesWithoutChangingTheInitialStartDate() {
        TeamMember member = activeMember();

        member.suspend();
        member.reactivate();

        assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
        assertEquals(STARTED_AT, member.getStartedAt());
        assertNull(member.getEndedAt());
    }

    @Test
    void removesAnInvitedMemberWithoutAStartDate() {
        TeamMember member = inviteMember();

        member.remove(ENDED_AT);

        assertEquals(TeamMemberStatus.REMOVED, member.getStatus());
        assertNull(member.getStartedAt());
        assertEquals(ENDED_AT, member.getEndedAt());
    }

    @Test
    void removesAnActiveMemberWithAnEndDate() {
        TeamMember member = activeMember();

        member.remove(ENDED_AT);

        assertEquals(TeamMemberStatus.REMOVED, member.getStatus());
        assertEquals(STARTED_AT, member.getStartedAt());
        assertEquals(ENDED_AT, member.getEndedAt());
    }

    @Test
    void acceptsAnEndDateEqualToTheStartDate() {
        TeamMember member = activeMember();

        member.remove(STARTED_AT);

        assertEquals(TeamMemberStatus.REMOVED, member.getStatus());
        assertEquals(STARTED_AT, member.getEndedAt());
    }

    @Test
    void rejectsAnEndDateBeforeTheStartDate() {
        TeamMember member = activeMember();

        assertThrows(IllegalArgumentException.class, () -> member.remove(STARTED_AT.minusSeconds(1)));

        assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
        assertNull(member.getEndedAt());
    }

    @Test
    void rejectsInvalidLifecycleOperations() {
        TeamMember invitedMember = inviteMember();
        TeamMember activeMember = activeMember();
        TeamMember suspendedMember = restoreMember(TeamMemberStatus.SUSPENDED, STARTED_AT, null);
        TeamMember removedMember = restoreMember(TeamMemberStatus.REMOVED, STARTED_AT, ENDED_AT);

        assertInvalidTransition(invitedMember::suspend);
        assertInvalidTransition(() -> activeMember.activate(ENDED_AT));
        assertInvalidTransition(activeMember::reactivate);
        assertInvalidTransition(() -> suspendedMember.activate(ENDED_AT));
        assertInvalidTransition(removedMember::suspend);
        assertInvalidTransition(removedMember::reactivate);
        assertInvalidTransition(() -> removedMember.remove(ENDED_AT));
    }

    @Test
    void rejectsNullDatesForOperationsThatRequireThem() {
        TeamMember invitedMember = inviteMember();
        TeamMember activeMember = activeMember();

        assertThrows(IllegalArgumentException.class, () -> invitedMember.activate(null));
        assertThrows(IllegalArgumentException.class, () -> activeMember.remove(null));
    }

    @Test
    void rejectsInvalidRestoredTemporalStates() {
        assertInvalidRestore(TeamMemberStatus.INVITED, STARTED_AT, null);
        assertInvalidRestore(TeamMemberStatus.INVITED, null, ENDED_AT);
        assertInvalidRestore(TeamMemberStatus.ACTIVE, null, null);
        assertInvalidRestore(TeamMemberStatus.ACTIVE, STARTED_AT, ENDED_AT);
        assertInvalidRestore(TeamMemberStatus.SUSPENDED, null, null);
        assertInvalidRestore(TeamMemberStatus.REMOVED, STARTED_AT, null);
        assertInvalidRestore(TeamMemberStatus.REMOVED, STARTED_AT, STARTED_AT.minusSeconds(1));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositivePersistenceIdentifiers(long id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> TeamMember.restore(
                        id, ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE, TeamMemberStatus.INVITED, null, null));
        assertThrows(
                IllegalArgumentException.class, () -> TeamMember.invite(ORGANIZATION_REFERENCE, id, USER_REFERENCE));
    }

    @Test
    void rejectsNullPersistenceIdentityOnRestore() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TeamMember.restore(
                        null, ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE, TeamMemberStatus.INVITED, null, null));
    }

    @Test
    void rejectsReferencesWithUnexpectedPrefixes() {
        assertThrows(IllegalArgumentException.class, () -> TeamMember.invite(USER_REFERENCE, TEAM_ID, USER_REFERENCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> TeamMember.invite(ORGANIZATION_REFERENCE, TEAM_ID, ORGANIZATION_REFERENCE));
    }

    private static TeamMember inviteMember() {
        return TeamMember.invite(ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE);
    }

    private static TeamMember activeMember() {
        return TeamMember.add(ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE, STARTED_AT);
    }

    private static TeamMember restoreMember(TeamMemberStatus status, Instant startedAt, Instant endedAt) {
        return TeamMember.restore(
                MEMBER_ID, ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE, status, startedAt, endedAt);
    }

    private static void assertRestoredState(TeamMemberStatus status, Instant startedAt, Instant endedAt) {
        TeamMember member = restoreMember(status, startedAt, endedAt);

        assertEquals(MEMBER_ID, member.getId());
        assertEquals(status, member.getStatus());
        assertEquals(startedAt, member.getStartedAt());
        assertEquals(endedAt, member.getEndedAt());
    }

    private static void assertInvalidRestore(TeamMemberStatus status, Instant startedAt, Instant endedAt) {
        assertThrows(IllegalArgumentException.class, () -> restoreMember(status, startedAt, endedAt));
    }

    private static void assertInvalidTransition(Runnable action) {
        TeamException exception = assertThrows(TeamException.class, action::run);

        assertEquals(TeamErrorCode.INVALID_MEMBER_STATUS_TRANSITION, exception.getErrorCode());
    }
}
