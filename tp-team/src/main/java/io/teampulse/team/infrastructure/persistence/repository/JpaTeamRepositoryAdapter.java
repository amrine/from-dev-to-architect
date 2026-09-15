package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaTeamRepositoryAdapter implements TeamRepository {
    @Override
    public Team create(Team team) {
        return null;
    }

    @Override
    public Team update(Team team) {
        return null;
    }

    @Override
    public Optional<Team> findByReference(String organizationReference, String teamReference) {
        return Optional.empty();
    }
}
