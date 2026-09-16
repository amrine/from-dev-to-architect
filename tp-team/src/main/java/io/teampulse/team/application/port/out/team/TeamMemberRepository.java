package io.teampulse.team.application.port.out.team;

import io.teampulse.team.domain.team.model.TeamMember;

import java.util.Optional;

public interface TeamMemberRepository {

    TeamMember create(TeamMember teamMember);

    TeamMember update(TeamMember teamMember);

    Optional<TeamMember> findCurrent(String organizationReference, Long teamId, String userReference);
}
