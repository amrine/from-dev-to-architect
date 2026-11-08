package io.teampulse.organization.application.service.organization;

import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.organization.AbstractOrganizationJpaIT;
import io.teampulse.organization.api.lifecycle.OrganizationProvisioningCommand;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleResult;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycle;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepository;
import io.teampulse.testsupport.persistence.MutableAuditDateTimeProvider;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationLifecycleServiceIT extends AbstractOrganizationJpaIT {

    private static final Instant CURRENT_INSTANT =
        Instant.parse("2026-09-14T08:00:00Z");

    @Inject
    private OrganizationLifecycle organizationLifecycle;

    @Inject
    private JpaOrganizationRepository jpaRepository;

    @Inject
    private MutableAuditDateTimeProvider auditDateTimeProvider;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaRepository::deleteAllInBatch);
        auditDateTimeProvider.setCurrentInstant(CURRENT_INSTANT);
    }

    @Test
    void createsAndReturnsThePersistedOrganization() {
        OrganizationLifecycleResult organization = organizationLifecycle.create(
            new OrganizationProvisioningCommand("  TeamPulse  ", "Europe/Paris")
        );

        assertTrue(ReferenceFormat.matches(organization.organizationReference(), "ORG"));
        assertEquals("TeamPulse", organization.name());
        assertEquals("CREATING", organization.status());

        OrganizationEntity storedEntity = findStoredOrganization(
            organization.organizationReference()
        );
        assertEquals(organization.organizationReference(), storedEntity.getReference());
        assertEquals("TeamPulse", storedEntity.getName());
        assertEquals(OrganizationStatus.CREATING, storedEntity.getStatus());
        assertEquals("SYSTEM", storedEntity.getCreatedBy());
        assertEquals("SYSTEM", storedEntity.getModifiedBy());
    }

    @Test
    void createsOrganizationInsideAnActiveReadWriteTransaction() {
        organizationLifecycle.create(
            new OrganizationProvisioningCommand("TeamPulse", "UTC")
        );

        TransactionManagerProbe.TransactionObservation observation =
            transactionProbe.observation();
        assertTrue(observation.name().endsWith("OrganizationLifecycleService.create"));
        assertFalse(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void rejectsInvalidCommandsBeforePersisting(OrganizationProvisioningCommand command) {
        assertThrows(
            ConstraintViolationException.class,
            () -> organizationLifecycle.create(command)
        );

        assertEquals(0L, jpaRepository.count());
    }

    @Test
    void archivesACreatingOrganizationAndPersistsTheTerminalState() {
        OrganizationLifecycleResult organization = organizationLifecycle.create(
            new OrganizationProvisioningCommand("TeamPulse", "UTC")
        );

        OrganizationLifecycleResult archived = organizationLifecycle.archive(
            organization.organizationReference()
        );

        assertEquals("ARCHIVED", archived.status());
        assertEquals(
            OrganizationStatus.ARCHIVED,
            findStoredOrganization(organization.organizationReference()).getStatus()
        );
    }

    static Stream<Arguments> invalidCommands() {
        return Stream.of(
            Arguments.of((OrganizationProvisioningCommand) null),
            Arguments.of(new OrganizationProvisioningCommand(" ", "UTC")),
            Arguments.of(new OrganizationProvisioningCommand("TeamPulse", " "))
        );
    }

    private OrganizationEntity findStoredOrganization(String reference) {
        return jpaRepository.findByReference(reference).orElseThrow();
    }
}
