package io.teampulse.organization.infrastructure.persistence.repository;

import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaOrganizationRepository
    extends JpaRepository<OrganizationEntity, Long> {

    /**
     * Queries the organization table using the functional reference.
     *
     * @param reference functional organization reference
     * @return the matching entity, or empty if absent
     */
    Optional<OrganizationEntity> findByReference(String reference);
}
