package io.teampulse.team.domain.team.model;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamTest {

    private static final Long TEAM_ID = 42L;
    private static final String TEAM_REFERENCE = "TEM-2026-1309-00000ZA7B900";
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B901";
    private static final String ADMIN_REFERENCE = "USR-2026-1309-00000ZA7B902";
    private static final String MANAGER_REFERENCE = "USR-2026-1309-00000ZA7B903";
    private static final String REPLACEMENT_REFERENCE = "USR-2026-1309-00000ZA7B904";

    @Test
    void createsAnActiveTeamWithoutPersistenceIdentity() {
        Team team = createTeam();

        assertNull(team.getId());
        assertEquals(TEAM_REFERENCE, team.getReference());
        assertEquals(ORGANIZATION_REFERENCE, team.getOrganizationReference());
        assertEquals("Platform", team.getName());
        assertEquals(ADMIN_REFERENCE, team.getAdminReference());
        assertEquals(MANAGER_REFERENCE, team.getManagerReference());
        assertEquals(TeamStatus.ACTIVE, team.getStatus());
    }

    @Test
    void restoresAStoredTeam() {
        Team team = Team.restore(
                TEAM_ID,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "Platform",
                ADMIN_REFERENCE,
                MANAGER_REFERENCE,
                TeamStatus.SUSPENDED);

        assertEquals(TEAM_ID, team.getId());
        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositivePersistenceIdentity(long id) {
        assertThrows(IllegalArgumentException.class, () -> restoreTeam(id, TeamStatus.ACTIVE));
    }

    @Test
    void rejectsNullPersistenceIdentityOnRestore() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Team.restore(
                        null,
                        TEAM_REFERENCE,
                        ORGANIZATION_REFERENCE,
                        "Platform",
                        ADMIN_REFERENCE,
                        MANAGER_REFERENCE,
                        TeamStatus.ACTIVE));
    }

    @Test
    void normalizesTheNameWithoutChangingItsCaseOrInternalSpaces() {
        Team team = Team.create(
                TEAM_REFERENCE, ORGANIZATION_REFERENCE, "  Platform  Team  ", ADMIN_REFERENCE, MANAGER_REFERENCE);

        assertEquals("Platform  Team", team.getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankName(String name) {
        TeamException exception = assertThrows(TeamException.class, () -> createTeam(name));

        assertEquals(TeamErrorCode.INVALID_NAME, exception.getErrorCode());
    }

    @Test
    void rejectsNullName() {
        TeamException exception = assertThrows(TeamException.class, () -> createTeam(null));

        assertEquals(TeamErrorCode.INVALID_NAME, exception.getErrorCode());
    }

    @Test
    void rejectsNameLongerThanTwoHundredCharacters() {
        TeamException exception = assertThrows(TeamException.class, () -> createTeam("A".repeat(201)));

        assertEquals(TeamErrorCode.INVALID_NAME, exception.getErrorCode());
    }

    @Test
    void acceptsNameWithTwoHundredCharacters() {
        Team team = createTeam("A".repeat(200));

        assertEquals("A".repeat(200), team.getName());
    }

    @Test
    void allowsTwoTeamsWithTheSameName() {
        Team firstTeam = createTeam();
        Team secondTeam = Team.create(
                "TEM-2026-1309-00000ZA7B905", ORGANIZATION_REFERENCE, "Platform", ADMIN_REFERENCE, MANAGER_REFERENCE);

        assertEquals(firstTeam.getName(), secondTeam.getName());
    }

    @Test
    void rejectsReferencesWithUnexpectedPrefixes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Team.create(
                        ORGANIZATION_REFERENCE,
                        ORGANIZATION_REFERENCE,
                        "Platform",
                        ADMIN_REFERENCE,
                        MANAGER_REFERENCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> Team.create(TEAM_REFERENCE, TEAM_REFERENCE, "Platform", ADMIN_REFERENCE, MANAGER_REFERENCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> Team.create(
                        TEAM_REFERENCE, ORGANIZATION_REFERENCE, "Platform", TEAM_REFERENCE, MANAGER_REFERENCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> Team.create(TEAM_REFERENCE, ORGANIZATION_REFERENCE, "Platform", ADMIN_REFERENCE, TEAM_REFERENCE));
    }

    @Test
    void allowsTheSameUserAsAdministratorAndManager() {
        Team team = Team.create(TEAM_REFERENCE, ORGANIZATION_REFERENCE, "Platform", ADMIN_REFERENCE, ADMIN_REFERENCE);

        assertEquals(ADMIN_REFERENCE, team.getAdminReference());
        assertEquals(ADMIN_REFERENCE, team.getManagerReference());
    }

    @Test
    void rejectsMissingResponsibleUsers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Team.create(TEAM_REFERENCE, ORGANIZATION_REFERENCE, "Platform", null, MANAGER_REFERENCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> Team.create(TEAM_REFERENCE, ORGANIZATION_REFERENCE, "Platform", ADMIN_REFERENCE, null));
    }

    @Test
    void suspendsAnActiveTeam() {
        Team team = createTeam();

        team.suspend();

        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
    }

    @Test
    void reactivatesASuspendedTeam() {
        Team team = restoreTeam(TEAM_ID, TeamStatus.SUSPENDED);

        team.reactivate();

        assertEquals(TeamStatus.ACTIVE, team.getStatus());
    }

    @Test
    void archivesAnActiveOrSuspendedTeam() {
        Team activeTeam = createTeam();
        Team suspendedTeam = restoreTeam(TEAM_ID, TeamStatus.SUSPENDED);

        activeTeam.archive();
        suspendedTeam.archive();

        assertEquals(TeamStatus.ARCHIVED, activeTeam.getStatus());
        assertEquals(TeamStatus.ARCHIVED, suspendedTeam.getStatus());
    }

    @Test
    void allowsResponsibleReplacementOnlyForActiveOrSuspendedTeams() {
        Team activeTeam = createTeam();
        Team suspendedTeam = restoreTeam(TEAM_ID, TeamStatus.SUSPENDED);
        Team archivedTeam = restoreTeam(TEAM_ID, TeamStatus.ARCHIVED);

        assertDoesNotThrow(() -> activeTeam.validateReplacementAllowed("replaceAdministrator"));
        assertDoesNotThrow(() -> suspendedTeam.validateReplacementAllowed("replaceManager"));

        TeamException exception = assertThrows(
                TeamException.class, () -> archivedTeam.validateReplacementAllowed("replaceAdministrator"));

        assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
    }

    @Test
    void rejectsTransitionsNotAllowedByTheLifecycle() {
        Team activeTeam = createTeam();
        Team suspendedTeam = restoreTeam(TEAM_ID, TeamStatus.SUSPENDED);
        Team archivedTeam = restoreTeam(TEAM_ID, TeamStatus.ARCHIVED);

        assertInvalidStatusTransition(activeTeam::reactivate);
        assertInvalidStatusTransition(suspendedTeam::suspend);
        assertInvalidStatusTransition(archivedTeam::suspend);
        assertInvalidStatusTransition(archivedTeam::reactivate);
        assertInvalidStatusTransition(archivedTeam::archive);
    }

    @Test
    void replacesAdministratorWithoutChangingTheStatus() {
        Team team = createTeam();

        team.replaceAdministrator(REPLACEMENT_REFERENCE);

        assertEquals(REPLACEMENT_REFERENCE, team.getAdminReference());
        assertEquals(MANAGER_REFERENCE, team.getManagerReference());
        assertEquals(TeamStatus.ACTIVE, team.getStatus());
    }

    @Test
    void replacesManagerOnASuspendedTeamWithoutChangingTheStatus() {
        Team team = restoreTeam(TEAM_ID, TeamStatus.SUSPENDED);

        team.replaceManager(REPLACEMENT_REFERENCE);

        assertEquals(ADMIN_REFERENCE, team.getAdminReference());
        assertEquals(REPLACEMENT_REFERENCE, team.getManagerReference());
        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
    }

    @Test
    void keepsTheTeamUnchangedWhenReplacingWithTheCurrentResponsible() {
        Team team = createTeam();

        team.replaceAdministrator(ADMIN_REFERENCE);
        team.replaceManager(MANAGER_REFERENCE);

        assertEquals(ADMIN_REFERENCE, team.getAdminReference());
        assertEquals(MANAGER_REFERENCE, team.getManagerReference());
        assertEquals(TeamStatus.ACTIVE, team.getStatus());
    }

    @Test
    void rejectsResponsibleReplacementAfterArchival() {
        Team team = restoreTeam(TEAM_ID, TeamStatus.ARCHIVED);

        assertInvalidStatusTransition(() -> team.replaceAdministrator(REPLACEMENT_REFERENCE));
        assertInvalidStatusTransition(() -> team.replaceManager(REPLACEMENT_REFERENCE));
        assertEquals(ADMIN_REFERENCE, team.getAdminReference());
        assertEquals(MANAGER_REFERENCE, team.getManagerReference());
    }

    @Test
    void rejectsInvalidResponsibleReferencesDuringReplacement() {
        Team team = createTeam();

        assertThrows(IllegalArgumentException.class, () -> team.replaceAdministrator(TEAM_REFERENCE));
        assertThrows(IllegalArgumentException.class, () -> team.replaceManager(ORGANIZATION_REFERENCE));
    }

    private static Team createTeam() {
        return createTeam("Platform");
    }

    private static Team createTeam(String name) {
        return Team.create(TEAM_REFERENCE, ORGANIZATION_REFERENCE, name, ADMIN_REFERENCE, MANAGER_REFERENCE);
    }

    private static Team restoreTeam(Long id, TeamStatus status) {
        return Team.restore(
                id, TEAM_REFERENCE, ORGANIZATION_REFERENCE, "Platform", ADMIN_REFERENCE, MANAGER_REFERENCE, status);
    }

    private static void assertInvalidStatusTransition(Runnable action) {
        TeamException exception = assertThrows(TeamException.class, action::run);

        assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
    }
}
