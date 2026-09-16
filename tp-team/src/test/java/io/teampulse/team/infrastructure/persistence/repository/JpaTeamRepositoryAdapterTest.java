package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.mapper.TeamPersistenceMapper;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaTeamRepositoryAdapterTest {

    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";

    @Mock(mockMaker = MockMakers.PROXY)
    private JpaTeamRepository jpaTeamRepository;

    @Mock(mockMaker = MockMakers.PROXY)
    private TeamPersistenceMapper teamPersistenceMapper;

    @InjectMocks
    private JpaTeamRepositoryAdapter adapter;

    @Test
    void translatesReferenceCollisionWithoutRetrying() {
        Team team = newTeam();
        TeamEntity entity = new TeamEntity();
        DataIntegrityViolationException persistenceFailure = constraintViolation("uk_teams_reference");
        when(teamPersistenceMapper.toEntity(team)).thenReturn(entity);
        when(jpaTeamRepository.saveAndFlush(any(TeamEntity.class))).thenThrow(persistenceFailure);

        TeamException exception = assertThrows(TeamException.class, () -> adapter.create(team));

        assertEquals(TeamErrorCode.REFERENCE_GENERATION_FAILED, exception.getErrorCode());
        assertSame(persistenceFailure, exception.getCause());
        ArgumentCaptor<TeamEntity> entityCaptor = ArgumentCaptor.forClass(TeamEntity.class);
        verify(jpaTeamRepository, times(1)).saveAndFlush(entityCaptor.capture());
        assertSame(entity, entityCaptor.getValue());
        verifyNoMoreInteractions(jpaTeamRepository);
    }

    @Test
    void translatesOptimisticLockConflictOnUpdate() {
        Team team = Team.restore(
                1L,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "TeamPulse Engineering",
                ADMIN_REFERENCE,
                MANAGER_REFERENCE,
                TeamStatus.SUSPENDED);
        TeamEntity entity = new TeamEntity();
        OptimisticLockingFailureException persistenceFailure =
                new OptimisticLockingFailureException("Concurrent update");
        when(jpaTeamRepository.findByOrganizationReferenceAndReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE))
                .thenReturn(Optional.of(entity));
        when(jpaTeamRepository.saveAndFlush(entity)).thenThrow(persistenceFailure);

        TeamException exception = assertThrows(TeamException.class, () -> adapter.update(team));

        assertEquals(TeamErrorCode.CONCURRENT_MODIFICATION, exception.getErrorCode());
        assertSame(persistenceFailure, exception.getCause());
        verify(teamPersistenceMapper).updateEntity(entity, team);
        verify(jpaTeamRepository).saveAndFlush(entity);
    }

    @Test
    void preservesUnknownConstraintAsTechnicalFailure() {
        Team team = newTeam();
        TeamEntity entity = new TeamEntity();
        DataIntegrityViolationException persistenceFailure = constraintViolation("fk_teams_unknown");
        when(teamPersistenceMapper.toEntity(team)).thenReturn(entity);
        when(jpaTeamRepository.saveAndFlush(entity)).thenThrow(persistenceFailure);

        DataIntegrityViolationException exception =
                assertThrows(DataIntegrityViolationException.class, () -> adapter.create(team));

        assertSame(persistenceFailure, exception);
    }

    private static Team newTeam() {
        return Team.create(
                TEAM_REFERENCE, ORGANIZATION_REFERENCE, "TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE);
    }

    private static DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "Persistence failure",
                new ConstraintViolationException(
                        "Constraint violation", new SQLException("Constraint violation", "23505"), constraintName));
    }
}
