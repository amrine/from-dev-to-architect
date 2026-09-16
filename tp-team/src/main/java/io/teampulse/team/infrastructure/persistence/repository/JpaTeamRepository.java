package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTeamRepository extends JpaRepository<TeamEntity, Long> {

    Optional<TeamEntity> findByOrganizationReferenceAndReference(String organizationReference, String reference);
}
