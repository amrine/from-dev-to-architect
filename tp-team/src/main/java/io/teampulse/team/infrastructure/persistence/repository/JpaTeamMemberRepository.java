package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface JpaTeamMemberRepository extends JpaRepository<TeamMemberEntity, Long> {

    Optional<TeamMemberEntity> findByOrganizationReferenceAndTeamIdAndUserReferenceAndStatusIn(
            String organizationReference, Long teamId, String userReference, Collection<TeamMemberStatus> statuses);

    Optional<TeamMemberEntity> findByIdAndOrganizationReferenceAndTeamIdAndUserReference(
            Long id, String organizationReference, Long teamId, String userReference);
}
