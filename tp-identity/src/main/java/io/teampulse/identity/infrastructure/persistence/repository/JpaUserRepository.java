package io.teampulse.identity.infrastructure.persistence.repository;

import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByOrganizationReferenceAndReference(String organizationReference, String reference);

    List<UserEntity> findAllByOrganizationReference(String organizationReference);

    boolean existsByOrganizationReferenceAndEmail(String organizationReference, String email);
}
