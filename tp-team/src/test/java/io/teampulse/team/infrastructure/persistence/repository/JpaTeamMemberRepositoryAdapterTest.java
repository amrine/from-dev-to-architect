package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import io.teampulse.team.infrastructure.persistence.mapper.TeamMemberPersistenceMapper;
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
import java.time.Instant;
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
class JpaTeamMemberRepositoryAdapterTest {

    private static final Long MEMBER_ID = 2L;
    private static final Long TEAM_ID = 1L;
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B900";
    private static final String USER_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final Instant STARTED_AT = Instant.parse("2026-09-16T10:15:30Z");

    @Mock(mockMaker = MockMakers.PROXY)
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @Mock(mockMaker = MockMakers.PROXY)
    private TeamMemberPersistenceMapper teamMemberPersistenceMapper;

    @InjectMocks
    private JpaTeamMemberRepositoryAdapter adapter;

    @Test
    void translatesCurrentMembershipCollisionWithoutRetrying() {
        TeamMember teamMember = TeamMember.invite(ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE);
        TeamMemberEntity entity = new TeamMemberEntity();
        DataIntegrityViolationException persistenceFailure = constraintViolation("uk_team_members_current");
        when(teamMemberPersistenceMapper.toEntity(teamMember)).thenReturn(entity);
        when(jpaTeamMemberRepository.saveAndFlush(any(TeamMemberEntity.class))).thenThrow(persistenceFailure);

        TeamException exception = assertThrows(TeamException.class, () -> adapter.create(teamMember));

        assertEquals(TeamErrorCode.MEMBER_ALREADY_EXISTS, exception.getErrorCode());
        assertSame(persistenceFailure, exception.getCause());
        ArgumentCaptor<TeamMemberEntity> entityCaptor = ArgumentCaptor.forClass(TeamMemberEntity.class);
        verify(jpaTeamMemberRepository, times(1)).saveAndFlush(entityCaptor.capture());
        assertSame(entity, entityCaptor.getValue());
        verifyNoMoreInteractions(jpaTeamMemberRepository);
    }

    @Test
    void translatesOptimisticLockConflictOnUpdate() {
        TeamMember teamMember = TeamMember.restore(
                MEMBER_ID, ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE, TeamMemberStatus.ACTIVE, STARTED_AT, null);
        TeamMemberEntity entity = new TeamMemberEntity();
        OptimisticLockingFailureException persistenceFailure =
                new OptimisticLockingFailureException("Concurrent update");
        when(jpaTeamMemberRepository.findByIdAndOrganizationReferenceAndTeamIdAndUserReference(
                        MEMBER_ID, ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE))
                .thenReturn(Optional.of(entity));
        when(jpaTeamMemberRepository.saveAndFlush(entity)).thenThrow(persistenceFailure);

        TeamException exception = assertThrows(TeamException.class, () -> adapter.update(teamMember));

        assertEquals(TeamErrorCode.CONCURRENT_MODIFICATION, exception.getErrorCode());
        assertSame(persistenceFailure, exception.getCause());
        verify(teamMemberPersistenceMapper).updateEntity(entity, teamMember);
        verify(jpaTeamMemberRepository).saveAndFlush(entity);
    }

    @Test
    void preservesUnknownConstraintAsTechnicalFailure() {
        TeamMember teamMember = TeamMember.invite(ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE);
        TeamMemberEntity entity = new TeamMemberEntity();
        DataIntegrityViolationException persistenceFailure = constraintViolation("fk_team_members_team");
        when(teamMemberPersistenceMapper.toEntity(teamMember)).thenReturn(entity);
        when(jpaTeamMemberRepository.saveAndFlush(entity)).thenThrow(persistenceFailure);

        DataIntegrityViolationException exception =
                assertThrows(DataIntegrityViolationException.class, () -> adapter.create(teamMember));

        assertSame(persistenceFailure, exception);
    }

    private static DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "Persistence failure",
                new ConstraintViolationException(
                        "Constraint violation", new SQLException("Constraint violation", "23505"), constraintName));
    }
}
