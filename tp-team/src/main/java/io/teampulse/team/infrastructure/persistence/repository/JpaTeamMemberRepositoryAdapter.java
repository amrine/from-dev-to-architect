package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.application.port.out.team.TeamMemberRepository;
import io.teampulse.team.domain.team.model.TeamMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaTeamMemberRepositoryAdapter implements TeamMemberRepository {
    @Override
    public TeamMember create(TeamMember teamMember) {
        return null;
    }

    @Override
    public TeamMember update(TeamMember teamMember) {
        return null;
    }

    @Override
    public Optional<TeamMember> findCurrent(String organizationReference, Long teamId, String userReference) {
        return Optional.empty();
    }
}
