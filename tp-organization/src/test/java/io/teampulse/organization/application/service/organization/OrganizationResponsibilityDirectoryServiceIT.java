package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.AbstractIntegrationTest;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilitySnapshot;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationResponsibilityDirectoryServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1409-00000ZA7B960";
    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1409-00000ZA7B960";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1409-00000ZA7B961";

    @Inject
    private OrganizationResponsibilityDirectory responsibilityDirectory;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private JpaOrganizationRepository jpaOrganizationRepository;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaOrganizationRepository::deleteAllInBatch);
    }

    @Test
    void returnsPublicLifecycleAndResponsibilityDataWithoutExposingAnOrganizationEntity() {
        organizationRepository.create(Organization.restore(
            ORGANIZATION_REFERENCE,
            "Acme",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.CREATING
        ));

        Optional<OrganizationResponsibilitySnapshot> result =
            responsibilityDirectory.findByOrganizationReference(ORGANIZATION_REFERENCE);

        assertTrue(result.isPresent());
        assertEquals(
            new OrganizationResponsibilitySnapshot(
                ORGANIZATION_REFERENCE,
                OrganizationLifecycleState.CREATING,
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE
            ),
            result.orElseThrow()
        );
    }

    @Test
    void returnsEmptyWhenTheOrganizationDoesNotExist() {
        assertFalse(responsibilityDirectory
            .findByOrganizationReference(ORGANIZATION_REFERENCE)
            .isPresent());
    }
}
