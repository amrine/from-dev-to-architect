package io.teampulse.organization.application.service.organization;

import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.organization.AbstractIntegrationTest;
import io.teampulse.organization.application.port.in.organization.CreateOrganizationCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationLifecycleUseCase;
import io.teampulse.organization.domain.organization.model.Organization;
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

class OrganizationLifecycleServiceIT extends AbstractIntegrationTest {

    private static final Instant CURRENT_INSTANT =
        Instant.parse("2026-09-14T08:00:00Z");

    @Inject
    private OrganizationLifecycleUseCase organizationLifecycle;

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
        Organization organization = organizationLifecycle.create(
            new CreateOrganizationCommand("  TeamPulse  ", "Europe/Paris")
        );

        assertTrue(ReferenceFormat.matches(organization.getReference(), "ORG"));
        assertEquals("TeamPulse", organization.getName());
        assertEquals(OrganizationStatus.CREATING, organization.getStatus());

        OrganizationEntity storedEntity = findStoredOrganization(
            organization.getReference()
        );
        assertEquals(organization.getReference(), storedEntity.getReference());
        assertEquals("TeamPulse", storedEntity.getName());
        assertEquals(OrganizationStatus.CREATING, storedEntity.getStatus());
        assertEquals("SYSTEM", storedEntity.getCreatedBy());
        assertEquals("SYSTEM", storedEntity.getModifiedBy());
    }

    @Test
    void createsOrganizationInsideAnActiveReadWriteTransaction() {
        organizationLifecycle.create(
            new CreateOrganizationCommand("TeamPulse", "UTC")
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
    void rejectsInvalidCommandsBeforePersisting(CreateOrganizationCommand command) {
        assertThrows(
            ConstraintViolationException.class,
            () -> organizationLifecycle.create(command)
        );

        assertEquals(0L, jpaRepository.count());
    }

    @Test
    void archivesACreatingOrganizationAndPersistsTheTerminalState() {
        Organization organization = organizationLifecycle.create(
            new CreateOrganizationCommand("TeamPulse", "UTC")
        );

        Organization archived = organizationLifecycle.archive(
            organization.getReference()
        );

        assertEquals(OrganizationStatus.ARCHIVED, archived.getStatus());
        assertEquals(
            OrganizationStatus.ARCHIVED,
            findStoredOrganization(organization.getReference()).getStatus()
        );
    }

    static Stream<Arguments> invalidCommands() {
        return Stream.of(
            Arguments.of((CreateOrganizationCommand) null),
            Arguments.of(new CreateOrganizationCommand(" ", "UTC")),
            Arguments.of(new CreateOrganizationCommand("TeamPulse", " "))
        );
    }

    private OrganizationEntity findStoredOrganization(String reference) {
        return jpaRepository.findByReference(reference).orElseThrow();
    }
}
