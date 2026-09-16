package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.AbstractIntegrationTest;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.testsupport.persistence.MutableAuditDateTimeProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaTeamRepositoryAdapterIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_A = "ORG-2026-0916-00000ZA7B900";
    private static final String ORGANIZATION_B = "ORG-2026-0916-00000ZA7B901";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";
    private static final Instant CREATED_AT = Instant.parse("2026-09-16T08:00:00Z");
    private static final Instant MODIFIED_AT = Instant.parse("2026-09-16T09:30:00Z");

    @Inject
    private JpaTeamRepositoryAdapter teamRepository;

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
    void persistsRestoresAndUpdatesTheTeamWithoutReplacingItsTechnicalState() {
        Team createdTeam = teamRepository.create(newTeam(ORGANIZATION_A));

        TeamEntity createdEntity = findEntity(ORGANIZATION_A, TEAM_REFERENCE);
        assertNotNull(createdTeam.getId());
        assertEquals(createdTeam.getId(), createdEntity.getId());
        assertEquals(0L, createdEntity.getVersion());
        assertEquals("SYSTEM", createdEntity.getCreatedBy());
        assertEquals(CREATED_AT, createdEntity.getCreatedAt());
        assertEquals(CREATED_AT, createdEntity.getModifiedAt());
        assertEquals("TeamPulse Engineering", createdEntity.getName());

        auditDateTimeProvider.setCurrentInstant(MODIFIED_AT);
        createdTeam.suspend();
        Team updatedTeam = teamRepository.update(createdTeam);

        TeamEntity updatedEntity = findEntity(ORGANIZATION_A, TEAM_REFERENCE);
        assertEquals(createdEntity.getId(), updatedEntity.getId());
        assertEquals(1L, updatedEntity.getVersion());
        assertEquals("SYSTEM", updatedEntity.getCreatedBy());
        assertEquals(CREATED_AT, updatedEntity.getCreatedAt());
        assertEquals("SYSTEM", updatedEntity.getModifiedBy());
        assertEquals(MODIFIED_AT, updatedEntity.getModifiedAt());
        assertEquals(TeamStatus.SUSPENDED, updatedEntity.getStatus());
        assertEquals(TeamStatus.SUSPENDED, updatedTeam.getStatus());
        assertTrue(
                teamRepository.findByReference(ORGANIZATION_A, TEAM_REFERENCE).isPresent());
        assertFalse(
                teamRepository.findByReference(ORGANIZATION_B, TEAM_REFERENCE).isPresent());
    }

    @Test
    void translatesReferenceCollisionAndPreservesTheExistingTeam() {
        teamRepository.create(newTeam(ORGANIZATION_A));

        TeamException exception =
                assertThrows(TeamException.class, () -> teamRepository.create(newTeam(ORGANIZATION_B)));

        assertEquals(TeamErrorCode.REFERENCE_GENERATION_FAILED, exception.getErrorCode());
        assertEquals(1L, jpaTeamRepository.count());
        assertEquals(ORGANIZATION_A, findEntity(ORGANIZATION_A, TEAM_REFERENCE).getOrganizationReference());
    }

    @Test
    void translatesOptimisticLockConflictWithoutOverwritingTheCommittedUpdate() {
        teamRepository.create(newTeam(ORGANIZATION_A));
        TransactionTemplate independentTransaction =
                new TransactionTemplate(transactionTemplate.getTransactionManager());
        independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        TeamException exception = assertThrows(
                TeamException.class,
                () -> transactionTemplate.executeWithoutResult(ignored -> {
                    TeamEntity staleEntity = findEntity(ORGANIZATION_A, TEAM_REFERENCE);

                    independentTransaction.executeWithoutResult(independentStatus -> {
                        TeamEntity currentEntity = findEntity(ORGANIZATION_A, TEAM_REFERENCE);
                        currentEntity.setName("First update");
                        jpaTeamRepository.save(currentEntity);
                    });

                    teamRepository.update(Team.restore(
                            staleEntity.getId(),
                            staleEntity.getReference(),
                            staleEntity.getOrganizationReference(),
                            staleEntity.getName(),
                            staleEntity.getAdminReference(),
                            staleEntity.getManagerReference(),
                            TeamStatus.SUSPENDED));
                }));

        assertEquals(TeamErrorCode.CONCURRENT_MODIFICATION, exception.getErrorCode());
        assertTrue(exception.getCause() instanceof OptimisticLockingFailureException);
        TeamEntity committedEntity = findEntity(ORGANIZATION_A, TEAM_REFERENCE);
        assertEquals("First update", committedEntity.getName());
        assertEquals(1L, committedEntity.getVersion());
    }

    private TeamEntity findEntity(String organizationReference, String teamReference) {
        return jpaTeamRepository
                .findByOrganizationReferenceAndReference(organizationReference, teamReference)
                .orElseThrow();
    }

    private static Team newTeam(String organizationReference) {
        return Team.create(
                TEAM_REFERENCE, organizationReference, "TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE);
    }
}
