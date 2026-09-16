package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.AbstractIntegrationTest;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import io.teampulse.testsupport.persistence.MutableAuditDateTimeProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaTeamMemberRepositoryAdapterIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_A = "ORG-2026-0916-00000ZA7B900";
    private static final String ORGANIZATION_B = "ORG-2026-0916-00000ZA7B901";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";
    private static final String USER_REFERENCE = "USR-2026-0916-00000ZA7B902";
    private static final Instant CREATED_AT = Instant.parse("2026-09-16T08:00:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-09-16T08:30:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-09-16T09:00:00Z");
    private static final Instant MODIFIED_AT = Instant.parse("2026-09-16T09:30:00Z");

    @Inject
    private JpaTeamRepositoryAdapter teamRepository;

    @Inject
    private JpaTeamMemberRepositoryAdapter teamMemberRepository;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @Inject
    private MutableAuditDateTimeProvider auditDateTimeProvider;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(() -> {
            jpaTeamMemberRepository.deleteAllInBatch();
            jpaTeamRepository.deleteAllInBatch();
        });
        auditDateTimeProvider.setCurrentInstant(CREATED_AT);
    }

    @Test
    void persistsMembershipHistoryAndExcludesRemovedMembershipsFromCurrentLookups() {
        Team team = teamRepository.create(newTeam());
        TeamMember invitedMember =
                teamMemberRepository.create(TeamMember.invite(ORGANIZATION_A, team.getId(), USER_REFERENCE));

        TeamMemberEntity invitedEntity = findEntity(invitedMember.getId());
        assertNotNull(invitedMember.getId());
        assertEquals(TeamMemberStatus.INVITED, invitedEntity.getStatus());
        assertEquals(0L, invitedEntity.getVersion());
        assertEquals(CREATED_AT, invitedEntity.getCreatedAt());
        assertTrue(teamMemberRepository
                .findCurrent(ORGANIZATION_A, team.getId(), USER_REFERENCE)
                .isPresent());

        invitedMember.activate(STARTED_AT);
        teamMemberRepository.update(invitedMember);
        assertEquals(
                TeamMemberStatus.ACTIVE,
                teamMemberRepository
                        .findCurrent(ORGANIZATION_A, team.getId(), USER_REFERENCE)
                        .orElseThrow()
                        .getStatus());

        invitedMember.suspend();
        teamMemberRepository.update(invitedMember);
        assertEquals(
                TeamMemberStatus.SUSPENDED,
                teamMemberRepository
                        .findCurrent(ORGANIZATION_A, team.getId(), USER_REFERENCE)
                        .orElseThrow()
                        .getStatus());

        invitedMember.remove(ENDED_AT);
        teamMemberRepository.update(invitedMember);

        assertFalse(teamMemberRepository
                .findCurrent(ORGANIZATION_A, team.getId(), USER_REFERENCE)
                .isPresent());
        TeamMember reinvitedMember =
                teamMemberRepository.create(TeamMember.invite(ORGANIZATION_A, team.getId(), USER_REFERENCE));

        assertNotEquals(invitedMember.getId(), reinvitedMember.getId());
        assertEquals(2L, jpaTeamMemberRepository.count());
        assertEquals(TeamMemberStatus.REMOVED, findEntity(invitedMember.getId()).getStatus());
        assertEquals(ENDED_AT, findEntity(invitedMember.getId()).getEndedAt());
    }

    @Test
    void translatesCurrentMembershipCollision() {
        Team team = teamRepository.create(newTeam());
        teamMemberRepository.create(TeamMember.invite(ORGANIZATION_A, team.getId(), USER_REFERENCE));

        TeamException exception = assertThrows(
                TeamException.class,
                () -> teamMemberRepository.create(TeamMember.invite(ORGANIZATION_A, team.getId(), USER_REFERENCE)));

        assertEquals(TeamErrorCode.MEMBER_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(1L, jpaTeamMemberRepository.count());
    }

    @Test
    void keepsForeignKeyViolationsAsTechnicalFailures() {
        Team team = teamRepository.create(newTeam());

        assertThrows(
                DataIntegrityViolationException.class,
                () -> teamMemberRepository.create(TeamMember.invite(ORGANIZATION_B, team.getId(), USER_REFERENCE)));
    }

    @Test
    void preservesAuditAndTechnicalIdentityWhenUpdatingMembership() {
        Team team = teamRepository.create(newTeam());
        TeamMember member =
                teamMemberRepository.create(TeamMember.add(ORGANIZATION_A, team.getId(), USER_REFERENCE, STARTED_AT));
        TeamMemberEntity createdEntity = findEntity(member.getId());

        auditDateTimeProvider.setCurrentInstant(MODIFIED_AT);
        member.suspend();
        TeamMember updatedMember = teamMemberRepository.update(member);

        TeamMemberEntity updatedEntity = findEntity(member.getId());
        assertEquals(createdEntity.getId(), updatedEntity.getId());
        assertEquals(1L, updatedEntity.getVersion());
        assertEquals("SYSTEM", updatedEntity.getCreatedBy());
        assertEquals(CREATED_AT, updatedEntity.getCreatedAt());
        assertEquals(MODIFIED_AT, updatedEntity.getModifiedAt());
        assertEquals(TeamMemberStatus.SUSPENDED, updatedEntity.getStatus());
        assertEquals(TeamMemberStatus.SUSPENDED, updatedMember.getStatus());
    }

    @Test
    void translatesOptimisticLockConflictWithoutOverwritingTheCommittedUpdate() {
        Team team = teamRepository.create(newTeam());
        TeamMember member =
                teamMemberRepository.create(TeamMember.add(ORGANIZATION_A, team.getId(), USER_REFERENCE, STARTED_AT));
        TransactionTemplate independentTransaction =
                new TransactionTemplate(transactionTemplate.getTransactionManager());
        independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        TeamException exception = assertThrows(
                TeamException.class,
                () -> transactionTemplate.executeWithoutResult(ignored -> {
                    TeamMemberEntity staleEntity = findEntity(member.getId());

                    independentTransaction.executeWithoutResult(independentStatus -> {
                        TeamMemberEntity currentEntity = findEntity(member.getId());
                        currentEntity.setStatus(TeamMemberStatus.SUSPENDED);
                        jpaTeamMemberRepository.save(currentEntity);
                    });

                    teamMemberRepository.update(TeamMember.restore(
                            staleEntity.getId(),
                            staleEntity.getOrganizationReference(),
                            staleEntity.getTeamId(),
                            staleEntity.getUserReference(),
                            TeamMemberStatus.SUSPENDED,
                            staleEntity.getStartedAt(),
                            null));
                }));

        assertEquals(TeamErrorCode.CONCURRENT_MODIFICATION, exception.getErrorCode());
        assertInstanceOf(OptimisticLockingFailureException.class, exception.getCause());
        TeamMemberEntity committedEntity = findEntity(member.getId());
        assertEquals(TeamMemberStatus.SUSPENDED, committedEntity.getStatus());
        assertEquals(1L, committedEntity.getVersion());
    }

    private TeamMemberEntity findEntity(Long memberId) {
        return jpaTeamMemberRepository.findById(memberId).orElseThrow();
    }

    private static Team newTeam() {
        return Team.create(TEAM_REFERENCE, ORGANIZATION_A, "TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE);
    }
}
