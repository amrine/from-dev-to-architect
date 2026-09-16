package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.team.AbstractIntegrationTest;
import io.teampulse.team.application.port.in.team.TeamMemberCommand;
import io.teampulse.team.application.port.in.team.TeamMembershipUseCase;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamMembershipServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_A = "ORG-2026-0916-00000ZA7B900";
    private static final String ORGANIZATION_B = "ORG-2026-0916-00000ZA7B901";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";
    private static final String MEMBER_REFERENCE = "USR-2026-0916-00000ZA7B902";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-16T08:30:00Z");

    @Inject
    private TeamMembershipUseCase teamMembership;

    @Inject
    private TeamRepository teamRepository;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @MockitoBean
    private OrganizationDirectory organizationDirectory;

    @MockitoBean
    private UserDirectory userDirectory;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(() -> {
            jpaTeamMemberRepository.deleteAllInBatch();
            jpaTeamRepository.deleteAllInBatch();
        });
        when(organizationDirectory.check(anyString())).thenReturn(OrganizationAvailability.AVAILABLE);
        when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.AVAILABLE);
    }

    @Test
    void addsAMemberUsingTheInjectedClockInsideAnActiveReadWriteTransaction() {
        Team team = givenActiveTeam();

        TeamMember member = teamMembership.addMember(tenantContext(ORGANIZATION_A), command());
        TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();

        assertEquals(TeamMemberStatus.ACTIVE, member.getStatus());
        assertEquals(FIXED_INSTANT, member.getStartedAt());
        assertEquals(1L, jpaTeamMemberRepository.count());
        verify(organizationDirectory).check(ORGANIZATION_A);
        verify(userDirectory).check(ORGANIZATION_A, MEMBER_REFERENCE);

        assertTrue(observation.name().endsWith("TeamMembershipService.addMember"));
        assertFalse(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    @Test
    void rejectsAnInvalidMembershipCommandBeforeCallingDirectoriesOrPersisting() {
        assertThrows(
                ConstraintViolationException.class,
                () -> teamMembership.addMember(
                        tenantContext(ORGANIZATION_A), new TeamMemberCommand(" ", MEMBER_REFERENCE)));

        assertEquals(0L, jpaTeamMemberRepository.count());
        verifyNoInteractions(organizationDirectory, userDirectory);
    }

    @Test
    void rejectsAnUnavailableMemberWithoutPersisting() {
        Team team = givenActiveTeam();
        when(userDirectory.check(ORGANIZATION_A, MEMBER_REFERENCE)).thenReturn(UserAvailability.UNAVAILABLE);

        TeamException exception = assertThrows(
                TeamException.class, () -> teamMembership.addMember(tenantContext(ORGANIZATION_A), command()));

        assertEquals(TeamErrorCode.MEMBER_USER_NOT_AVAILABLE, exception.getErrorCode());
        assertEquals(0L, jpaTeamMemberRepository.count());
        verify(organizationDirectory).check(ORGANIZATION_A);
        verify(userDirectory).check(ORGANIZATION_A, MEMBER_REFERENCE);
    }

    @Test
    void isolatesMembershipOperationsFromAnotherTenant() {
        givenActiveTeam();

        TeamException exception = assertThrows(
                TeamException.class, () -> teamMembership.addMember(tenantContext(ORGANIZATION_B), command()));

        assertEquals(TeamErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals(0L, jpaTeamMemberRepository.count());
        verifyNoInteractions(organizationDirectory, userDirectory);
    }

    private Team givenActiveTeam() {
        return teamRepository.create(Team.create(
                TEAM_REFERENCE, ORGANIZATION_A, "TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE));
    }

    private static TenantContext tenantContext(String organizationReference) {
        return new TenantContext(organizationReference);
    }

    private static TeamMemberCommand command() {
        return new TeamMemberCommand(TEAM_REFERENCE, MEMBER_REFERENCE);
    }
}
