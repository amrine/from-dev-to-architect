package io.teampulse.organization.infrastructure.persistence.repository;

import io.teampulse.organization.AbstractIntegrationTest;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.testsupport.persistence.MutableAuditDateTimeProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaOrganizationRepositoryAdapterIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE_1 =
        "ORG-2026-1309-00000ZA7B900";
    private static final String ORGANIZATION_REFERENCE_2 =
        "ORG-2026-1309-00000ZA7B901";
    private static final String ADMIN_REFERENCE =
        "USR-2026-1309-00000ZA7B900";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1309-00000ZA7B901";
    private static final Instant CREATED_AT =
        Instant.parse("2026-09-13T08:00:00Z");
    private static final Instant MODIFIED_AT =
        Instant.parse("2026-09-13T09:30:00Z");

    @Inject
    private JpaOrganizationRepositoryAdapter organizationRepository;

    @Inject
    private JpaOrganizationRepository jpaRepository;

    @Inject
    private MutableAuditDateTimeProvider auditDateTimeProvider;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaRepository::deleteAllInBatch);
        auditDateTimeProvider.setCurrentInstant(CREATED_AT);
    }

    @Nested
    class MappingTests {

        @Test
        void persistsAndRestoresTheCompleteBusinessState() {
            Organization organization = activeOrganization(
                ORGANIZATION_REFERENCE_1,
                "TeamPulse"
            );

            Organization createdOrganization =
                organizationRepository.create(organization);
            Organization restoredOrganization = organizationRepository
                .findByReference(ORGANIZATION_REFERENCE_1)
                .orElseThrow();
            OrganizationEntity storedEntity = findEntity(
                ORGANIZATION_REFERENCE_1
            );

            assertOrganizationState(createdOrganization);
            assertOrganizationState(restoredOrganization);
            assertNotNull(storedEntity.getId());
            assertEquals(ORGANIZATION_REFERENCE_1, storedEntity.getReference());
            assertEquals("TeamPulse", storedEntity.getName());
            assertEquals("Europe/Paris", storedEntity.getTimezone());
            assertEquals(ADMIN_REFERENCE, storedEntity.getAdminReference());
            assertEquals(MANAGER_REFERENCE, storedEntity.getManagerReference());
            assertEquals(OrganizationStatus.ACTIVE, storedEntity.getStatus());
        }

        @Test
        void returnsEmptyWhenTheOrganizationDoesNotExist() {
            assertTrue(
                organizationRepository
                    .findByReference(ORGANIZATION_REFERENCE_1)
                    .isEmpty()
            );
        }
    }

    @Nested
    class UniquenessTests {

        @Test
        void rejectsAReferenceCollisionWithoutChangingTheExistingOrganization() {
            organizationRepository.create(
                Organization.create(
                    ORGANIZATION_REFERENCE_1,
                    "Existing organization",
                    "Europe/Paris"
                )
            );
            Long originalId = findEntity(ORGANIZATION_REFERENCE_1).getId();

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> organizationRepository.create(
                    Organization.create(
                        ORGANIZATION_REFERENCE_1,
                        "Conflicting organization",
                        "UTC"
                    )
                )
            );

            assertEquals(
                OrganizationErrorCode.REFERENCE_GENERATION_FAILED,
                exception.getErrorCode()
            );
            assertEquals(1L, jpaRepository.count());
            OrganizationEntity originalEntity = findEntity(
                ORGANIZATION_REFERENCE_1
            );
            assertEquals(originalId, originalEntity.getId());
            assertEquals("Existing organization", originalEntity.getName());
            assertEquals("Europe/Paris", originalEntity.getTimezone());
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void rejectsAnUpdateWhenTheOrganizationDoesNotExist() {
            Organization organization = Organization.create(
                ORGANIZATION_REFERENCE_2,
                "Unknown organization",
                "UTC"
            );

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> organizationRepository.update(organization)
            );

            assertEquals(
                OrganizationErrorCode.NOT_FOUND,
                exception.getErrorCode()
            );
        }

        @Test
        void preservesTechnicalStateAndPropagatesAnAbsentManager() {
            Organization organization = organizationRepository.create(
                activeOrganization(ORGANIZATION_REFERENCE_1, "TeamPulse")
            );
            OrganizationEntity createdEntity = findEntity(
                ORGANIZATION_REFERENCE_1
            );

            assertEquals(0L, createdEntity.getVersion());
            assertEquals("SYSTEM", createdEntity.getCreatedBy());
            assertEquals(CREATED_AT, createdEntity.getCreatedAt());
            assertEquals("SYSTEM", createdEntity.getModifiedBy());
            assertEquals(CREATED_AT, createdEntity.getModifiedAt());

            auditDateTimeProvider.setCurrentInstant(MODIFIED_AT);
            organization.removeManager();
            Organization updatedOrganization =
                organizationRepository.update(organization);
            OrganizationEntity updatedEntity = findEntity(
                ORGANIZATION_REFERENCE_1
            );

            assertEquals(createdEntity.getId(), updatedEntity.getId());
            assertEquals(1L, updatedEntity.getVersion());
            assertEquals("SYSTEM", updatedEntity.getCreatedBy());
            assertEquals(CREATED_AT, updatedEntity.getCreatedAt());
            assertEquals("SYSTEM", updatedEntity.getModifiedBy());
            assertEquals(MODIFIED_AT, updatedEntity.getModifiedAt());
            assertEquals(ADMIN_REFERENCE, updatedEntity.getAdminReference());
            assertNull(updatedEntity.getManagerReference());
            assertEquals(
                OrganizationStatus.SUSPENDED,
                updatedEntity.getStatus()
            );
            assertNull(updatedOrganization.getManagerReference());
            assertEquals(
                OrganizationStatus.SUSPENDED,
                updatedOrganization.getStatus()
            );
        }
    }

    @Nested
    class OptimisticLockingTests {

        @Test
        void translatesAConflictWithoutOverwritingTheCommittedUpdate() {
            organizationRepository.create(
                activeOrganization(ORGANIZATION_REFERENCE_1, "TeamPulse")
            );
            TransactionTemplate independentTransaction =
                new TransactionTemplate(transactionTemplate.getTransactionManager());
            independentTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
            );

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> transactionTemplate.executeWithoutResult(ignored -> {
                    OrganizationEntity staleEntity = findEntity(
                        ORGANIZATION_REFERENCE_1
                    );
                    assertEquals(0L, staleEntity.getVersion());

                    independentTransaction.executeWithoutResult(
                        independentStatus -> {
                            OrganizationEntity currentEntity = findEntity(
                                ORGANIZATION_REFERENCE_1
                            );
                            assertNotSame(staleEntity, currentEntity);
                            currentEntity.setName("Committed update");
                            jpaRepository.save(currentEntity);
                        }
                    );

                    organizationRepository.update(
                        activeOrganization(
                            ORGANIZATION_REFERENCE_1,
                            "Stale update"
                        )
                    );
                })
            );

            assertEquals(
                OrganizationErrorCode.CONCURRENT_MODIFICATION,
                exception.getErrorCode()
            );
            assertInstanceOf(
                OptimisticLockingFailureException.class,
                exception.getCause()
            );
            OrganizationEntity committedEntity = findEntity(
                ORGANIZATION_REFERENCE_1
            );
            assertEquals("Committed update", committedEntity.getName());
            assertEquals(1L, committedEntity.getVersion());
        }
    }

    @Nested
    class DatabaseConstraintTests {

        @Test
        void rejectsNamesLongerThanTwoHundredCharacters() {
            assertThrows(
                DataIntegrityViolationException.class,
                () -> jpaRepository.saveAndFlush(entity(
                    ORGANIZATION_REFERENCE_1,
                    "A".repeat(201),
                    OrganizationStatus.CREATING,
                    null,
                    null
                ))
            );
        }

        @Test
        void rejectsActiveOrganizationsWithoutBothResponsibleReferences() {
            assertThrows(
                DataIntegrityViolationException.class,
                () -> jpaRepository.saveAndFlush(entity(
                    ORGANIZATION_REFERENCE_1,
                    "TeamPulse",
                    OrganizationStatus.ACTIVE,
                    null,
                    null
                ))
            );
        }

        @Test
        void rejectsSuspendedOrganizationsWithoutAnAdministrator() {
            assertThrows(
                DataIntegrityViolationException.class,
                () -> jpaRepository.saveAndFlush(entity(
                    ORGANIZATION_REFERENCE_1,
                    "TeamPulse",
                    OrganizationStatus.SUSPENDED,
                    null,
                    MANAGER_REFERENCE
                ))
            );
        }

        @Test
        void acceptsSuspendedOrganizationsWithoutAManager() {
            assertDoesNotThrow(
                () -> jpaRepository.saveAndFlush(entity(
                    ORGANIZATION_REFERENCE_1,
                    "TeamPulse",
                    OrganizationStatus.SUSPENDED,
                    ADMIN_REFERENCE,
                    null
                ))
            );
        }

        @Test
        void acceptsArchivedOrganizationsWithoutResponsibleReferences() {
            assertDoesNotThrow(
                () -> jpaRepository.saveAndFlush(entity(
                    ORGANIZATION_REFERENCE_1,
                    "TeamPulse",
                    OrganizationStatus.ARCHIVED,
                    null,
                    null
                ))
            );
        }

        @Test
        void generatesIdsFromTheOrganizationSequence() {
            OrganizationEntity first = jpaRepository.saveAndFlush(entity(
                ORGANIZATION_REFERENCE_1,
                "First organization",
                OrganizationStatus.CREATING,
                null,
                null
            ));
            OrganizationEntity second = jpaRepository.saveAndFlush(entity(
                ORGANIZATION_REFERENCE_2,
                "Second organization",
                OrganizationStatus.CREATING,
                null,
                null
            ));

            assertEquals(first.getId() + 1, second.getId());
        }

        @Test
        void doesNotRequireResponsibleUsersToExistInAnotherModule() {
            assertDoesNotThrow(
                () -> jpaRepository.saveAndFlush(entity(
                    ORGANIZATION_REFERENCE_1,
                    "TeamPulse",
                    OrganizationStatus.ACTIVE,
                    ADMIN_REFERENCE,
                    MANAGER_REFERENCE
                ))
            );
        }
    }

    private OrganizationEntity findEntity(String organizationReference) {
        return jpaRepository.findByReference(organizationReference)
            .orElseThrow(NoSuchElementException::new);
    }

    private static Organization activeOrganization(
        String organizationReference,
        String name
    ) {
        return Organization.restore(
            organizationReference,
            name,
            "Europe/Paris",
            ADMIN_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.ACTIVE
        );
    }

    private static OrganizationEntity entity(
        String reference,
        String name,
        OrganizationStatus status,
        String adminReference,
        String managerReference
    ) {
        return new OrganizationEntity()
            .setReference(reference)
            .setName(name)
            .setTimezone("Europe/Paris")
            .setAdminReference(adminReference)
            .setManagerReference(managerReference)
            .setStatus(status);
    }

    private static void assertOrganizationState(Organization organization) {
        assertEquals(ORGANIZATION_REFERENCE_1, organization.getReference());
        assertEquals("TeamPulse", organization.getName());
        assertEquals(ZoneId.of("Europe/Paris"), organization.getTimezone());
        assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
        assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
        assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
    }
}
