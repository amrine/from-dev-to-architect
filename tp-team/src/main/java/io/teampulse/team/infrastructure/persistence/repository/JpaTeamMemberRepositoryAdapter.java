package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.common.persistence.ConstraintNameExtractor;
import io.teampulse.team.application.port.out.team.TeamMemberRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import io.teampulse.team.infrastructure.persistence.mapper.TeamMemberPersistenceMapper;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class JpaTeamMemberRepositoryAdapter implements TeamMemberRepository {

    private static final String CURRENT_MEMBER_UNIQUE_CONSTRAINT = "uk_team_members_current";
    private static final Set<TeamMemberStatus> CURRENT_STATUSES =
            EnumSet.of(TeamMemberStatus.INVITED, TeamMemberStatus.ACTIVE, TeamMemberStatus.SUSPENDED);

    private final JpaTeamMemberRepository jpaTeamMemberRepository;
    private final TeamMemberPersistenceMapper teamMemberPersistenceMapper;

    @Override
    public TeamMember create(TeamMember teamMember) {
        try {
            TeamMemberEntity entity = teamMemberPersistenceMapper.toEntity(teamMember);

            return teamMemberPersistenceMapper.toDomain(jpaTeamMemberRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    @Override
    public TeamMember update(TeamMember teamMember) {
        try {
            TeamMemberEntity entity = jpaTeamMemberRepository
                    .findByIdAndOrganizationReferenceAndTeamIdAndUserReference(
                            teamMember.getId(),
                            teamMember.getOrganizationReference(),
                            teamMember.getTeamId(),
                            teamMember.getUserReference())
                    .orElseThrow(() -> new TeamException(TeamErrorCode.MEMBER_NOT_FOUND, "Team member was not found"));

            teamMemberPersistenceMapper.updateEntity(entity, teamMember);
            return teamMemberPersistenceMapper.toDomain(jpaTeamMemberRepository.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new TeamException(
                    TeamErrorCode.CONCURRENT_MODIFICATION, "Team member was modified concurrently", exception);
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    @Override
    public Optional<TeamMember> findCurrent(String organizationReference, Long teamId, String userReference) {
        return jpaTeamMemberRepository
                .findByOrganizationReferenceAndTeamIdAndUserReferenceAndStatusIn(
                        organizationReference, teamId, userReference, CURRENT_STATUSES)
                .map(teamMemberPersistenceMapper::toDomain);
    }

    private RuntimeException translatePersistenceException(RuntimeException exception) {
        return ConstraintNameExtractor.extract(
                        exception, ConstraintViolationException.class, ConstraintViolationException::getConstraintName)
                .filter(CURRENT_MEMBER_UNIQUE_CONSTRAINT::equals)
                .<RuntimeException>map(_ -> new TeamException(
                        TeamErrorCode.MEMBER_ALREADY_EXISTS, "Current team membership already exists", exception))
                .orElse(exception);
    }
}
