package io.teampulse.organization.infrastructure.persistence.repository;

import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaOrganizationRepository
    extends JpaRepository<OrganizationEntity, Long> {

    Optional<OrganizationEntity> findByReference(String reference);
}
