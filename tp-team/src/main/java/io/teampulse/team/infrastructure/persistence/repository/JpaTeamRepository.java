package io.teampulse.team.infrastructure.persistence.repository;

import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaTeamRepository extends JpaRepository<TeamEntity, Long> {

    Optional<TeamEntity> findByOrganizationReferenceAndReference(String organizationReference, String reference);

    @Query("""
        select team
        from TeamEntity team
        where team.organizationReference = :organizationReference
          and (team.adminReference = :userReference or team.managerReference = :userReference)
        order by team.reference
        """)
    List<TeamEntity> findByOrganizationReferenceAndResponsibleUser(
        @Param("organizationReference") String organizationReference,
        @Param("userReference") String userReference
    );
}
