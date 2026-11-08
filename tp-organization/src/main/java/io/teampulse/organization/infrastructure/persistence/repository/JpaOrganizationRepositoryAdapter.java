package io.teampulse.organization.infrastructure.persistence.repository;

import io.teampulse.common.persistence.ConstraintNameExtractor;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.organization.infrastructure.persistence.mapper.OrganizationPersistenceMapper;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaOrganizationRepositoryAdapter implements OrganizationRepository {

    private static final String REFERENCE_UNIQUE_CONSTRAINT =
        "uk_organizations_reference";

    private final JpaOrganizationRepository jpaOrganizationRepository;
    private final OrganizationPersistenceMapper organizationPersistenceMapper;

    /**
     * {@inheritDoc}
     *
     * <p>Flushes the insert within this adapter so a known reference collision can
     * be translated before leaving the persistence boundary. Unknown failures propagate.</p>
     */
    @Override
    public Organization create(Organization organization) {
        try {
            OrganizationEntity organizationEntity =
                organizationPersistenceMapper.toEntity(organization);

            // Flush before leaving the adapter so database constraint violations
            // can still be translated within this boundary.
            return organizationPersistenceMapper.toDomain(
                jpaOrganizationRepository.saveAndFlush(organizationEntity)
            );
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the managed entity, copies business state and flushes the update.
     * Optimistic locking failures become organization concurrency errors with their cause
     * retained.</p>
     */
    @Override
    public Organization update(Organization organization) {
        try {
            OrganizationEntity organizationEntity =
                jpaOrganizationRepository.findByReference(
                    organization.getReference()
                ).orElseThrow(() -> new OrganizationException(
                    OrganizationErrorCode.NOT_FOUND,
                    "Organization was not found: " + organization.getReference()
                ));

            organizationPersistenceMapper.updateEntity(
                organizationEntity,
                organization
            );

            // Flush before leaving the adapter so optimistic locking and database
            // constraint failures can still be translated within this boundary.
            return organizationPersistenceMapper.toDomain(
                jpaOrganizationRepository.saveAndFlush(organizationEntity)
            );
        } catch (
            OptimisticLockingFailureException | OptimisticLockException exception
        ) {
            throw new OrganizationException(
                OrganizationErrorCode.CONCURRENT_MODIFICATION,
                "Organization was modified concurrently",
                exception
            );
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Restores business state through the persistence mapper; no entity escapes this
     * adapter.</p>
     */
    @Override
    public Optional<Organization> findByReference(
        String organizationReference
    ) {
        return jpaOrganizationRepository.findByReference(organizationReference)
            .map(organizationPersistenceMapper::toDomain);
    }

    /**
     * Translates the known unique-reference constraint and retains its cause.
     * An unrecognized persistence failure is returned unchanged as a technical error.
     *
     * @param exception persistence failure to inspect
     * @return the translated domain failure, or the original exception
     */
    private RuntimeException translatePersistenceException(
        RuntimeException exception
    ) {
        return ConstraintNameExtractor.extract(
                exception,
                ConstraintViolationException.class,
                ConstraintViolationException::getConstraintName
            )
            .filter(REFERENCE_UNIQUE_CONSTRAINT::equals)
            .<RuntimeException>map(_ -> new OrganizationException(
                OrganizationErrorCode.REFERENCE_GENERATION_FAILED,
                "Generated organization reference already exists",
                exception
            ))
            .orElse(exception);
    }
}
