package io.teampulse.organization.infrastructure.persistence.repository;

import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Temporary persistence adapter used while the organization schema is being
 * implemented.
 *
 * <p>The entity, Spring Data repository and persistence mapper will be added
 * in the persistence implementation step. Until then, every operation fails
 * explicitly instead of returning an incomplete or misleading result.</p>
 */
@Repository
@AllArgsConstructor
public class JpaOrganizationRepositoryAdapter implements OrganizationRepository {

    private static final String NOT_IMPLEMENTED =
        "Organization persistence is not implemented yet";

    @Override
    public Organization create(Organization organization) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Organization update(Organization organization) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Optional<Organization> findByReference(
        String organizationReference
    ) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
