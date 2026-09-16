package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.common.persistence.ConstraintNameExtractor;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.mapper.TeamPersistenceMapper;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaTeamRepositoryAdapter implements TeamRepository {

    private static final String REFERENCE_UNIQUE_CONSTRAINT = "uk_teams_reference";

    private final JpaTeamRepository jpaTeamRepository;
    private final TeamPersistenceMapper teamPersistenceMapper;

    @Override
    public Team create(Team team) {
        try {
            TeamEntity entity = teamPersistenceMapper.toEntity(team);

            return teamPersistenceMapper.toDomain(jpaTeamRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    @Override
    public Team update(Team team) {
        try {
            TeamEntity entity = jpaTeamRepository
                    .findByOrganizationReferenceAndReference(team.getOrganizationReference(), team.getReference())
                    .orElseThrow(() ->
                            new TeamException(TeamErrorCode.NOT_FOUND, "Team was not found: " + team.getReference()));

            teamPersistenceMapper.updateEntity(entity, team);
            return teamPersistenceMapper.toDomain(jpaTeamRepository.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new TeamException(TeamErrorCode.CONCURRENT_MODIFICATION, "Team was modified concurrently", exception);
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    @Override
    public Optional<Team> findByReference(String organizationReference, String teamReference) {
        return jpaTeamRepository
                .findByOrganizationReferenceAndReference(organizationReference, teamReference)
                .map(teamPersistenceMapper::toDomain);
    }

    private RuntimeException translatePersistenceException(RuntimeException exception) {
        return ConstraintNameExtractor.extract(
                        exception, ConstraintViolationException.class, ConstraintViolationException::getConstraintName)
                .filter(REFERENCE_UNIQUE_CONSTRAINT::equals)
                .<RuntimeException>map(_ -> new TeamException(
                        TeamErrorCode.REFERENCE_GENERATION_FAILED,
                        "Generated team reference already exists",
                        exception))
                .orElse(exception);
    }
}
