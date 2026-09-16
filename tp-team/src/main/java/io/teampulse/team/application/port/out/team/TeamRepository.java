package io.teampulse.team.application.port.out.team;

import io.teampulse.team.domain.team.model.Team;

import java.util.Optional;

public interface TeamRepository {

    Team create(Team team);

    Team update(Team team);

    Optional<Team> findByReference(String organizationReference, String teamReference);
}
