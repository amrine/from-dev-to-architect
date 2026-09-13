package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.AbstractIntegrationTest;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepository;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepositoryAdapter;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationDirectoryServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1409-00000ZA7B910";
    private static final String ADMIN_REFERENCE =
        "USR-2026-1409-00000ZA7B910";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1409-00000ZA7B911";

    @Inject
    private OrganizationDirectory organizationDirectory;

    @Inject
    private JpaOrganizationRepositoryAdapter organizationRepository;

    @Inject
    private JpaOrganizationRepository jpaRepository;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaRepository::deleteAllInBatch);
    }

    @ParameterizedTest
    @MethodSource("organizationsByStatus")
    void mapsPersistedStatusToPublicAvailability(
        Organization organization,
        OrganizationAvailability expectedAvailability
    ) {
        organizationRepository.create(organization);

        assertEquals(
            expectedAvailability,
            organizationDirectory.check(ORGANIZATION_REFERENCE)
        );
    }

    @Test
    void returnsNotFoundWhenOrganizationDoesNotExist() {
        assertEquals(
            OrganizationAvailability.NOT_FOUND,
            organizationDirectory.check(ORGANIZATION_REFERENCE)
        );
    }

    @Test
    void checksAvailabilityInsideAnActiveReadOnlyTransaction() {
        organizationDirectory.check(ORGANIZATION_REFERENCE);

        TransactionManagerProbe.TransactionObservation observation =
            transactionProbe.observation();
        assertTrue(observation.name().endsWith("OrganizationDirectoryService.check"));
        assertTrue(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    @ParameterizedTest
    @MethodSource("invalidOrganizationReferences")
    void rejectsInvalidReferencesAsContractViolations(
        String organizationReference
    ) {
        RuntimeException exception = assertThrows(
            ConstraintViolationException.class,
            () -> organizationDirectory.check(organizationReference)
        );
        assertFalse(exception instanceof OrganizationDirectoryException);
    }

    static Stream<String> invalidOrganizationReferences() {
        return Stream.of(null, "", "   ");
    }

    static Stream<Arguments> organizationsByStatus() {
        return Stream.of(
            Arguments.of(
                Organization.create(
                    ORGANIZATION_REFERENCE,
                    "Creating organization",
                    "Europe/Paris"
                ),
                OrganizationAvailability.UNAVAILABLE
            ),
            Arguments.of(
                Organization.restore(
                    ORGANIZATION_REFERENCE,
                    "Active organization",
                    "Europe/Paris",
                    ADMIN_REFERENCE,
                    MANAGER_REFERENCE,
                    OrganizationStatus.ACTIVE
                ),
                OrganizationAvailability.AVAILABLE
            ),
            Arguments.of(
                Organization.restore(
                    ORGANIZATION_REFERENCE,
                    "Suspended organization",
                    "Europe/Paris",
                    ADMIN_REFERENCE,
                    null,
                    OrganizationStatus.SUSPENDED
                ),
                OrganizationAvailability.UNAVAILABLE
            ),
            Arguments.of(
                Organization.restore(
                    ORGANIZATION_REFERENCE,
                    "Archived organization",
                    "Europe/Paris",
                    null,
                    null,
                    OrganizationStatus.ARCHIVED
                ),
                OrganizationAvailability.UNAVAILABLE
            )
        );
    }
}
