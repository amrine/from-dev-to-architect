package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.AbstractIntegrationTest;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleUsersUseCase;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class OrganizationResponsibleUsersServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1409-00000ZA7B930";
    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1409-00000ZA7B930";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1409-00000ZA7B931";
    private static final String OTHER_MANAGER_REFERENCE =
        "USR-2026-1409-00000ZA7B932";

    @Inject
    private OrganizationResponsibleUsersUseCase responsibleUsers;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private JpaOrganizationRepository jpaRepository;

    @MockitoBean
    private UserDirectory userDirectory;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaRepository::deleteAllInBatch);
        when(userDirectory.check(anyString(), anyString()))
            .thenReturn(UserAvailability.NOT_FOUND);
    }

    @Test
    void assignsBothAvailableUsersAndActivatesTheOrganization() {
        givenOrganization(creatingOrganization());
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE
        )).thenReturn(UserAvailability.AVAILABLE);
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            MANAGER_REFERENCE
        )).thenReturn(UserAvailability.AVAILABLE);

        responsibleUsers.assignAdministrator(command(ADMINISTRATOR_REFERENCE));
        Organization result = responsibleUsers.assignManager(
            command(MANAGER_REFERENCE)
        );

        assertEquals(OrganizationStatus.ACTIVE, result.getStatus());
        assertEquals(
            OrganizationStatus.ACTIVE,
            findStoredOrganization().getStatus()
        );
    }

    @Test
    void assignsPendingManagerWithoutActivatingTheOrganization() {
        givenOrganization(creatingOrganizationWithAdministrator());
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            MANAGER_REFERENCE
        )).thenReturn(UserAvailability.PENDING);

        Organization result = responsibleUsers.assignManager(
            command(MANAGER_REFERENCE)
        );

        assertEquals(OrganizationStatus.CREATING, result.getStatus());
        assertEquals(
            OrganizationStatus.CREATING,
            findStoredOrganization().getStatus()
        );
    }

    @Test
    void replacesManagerWithoutChangingTheOrganizationStatus() {
        givenOrganization(activeOrganization());
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            OTHER_MANAGER_REFERENCE
        )).thenReturn(UserAvailability.AVAILABLE);

        Organization result = responsibleUsers.replaceManager(
            command(OTHER_MANAGER_REFERENCE)
        );

        assertEquals(OrganizationStatus.ACTIVE, result.getStatus());
        assertEquals(
            OTHER_MANAGER_REFERENCE,
            findStoredOrganization().getManagerReference()
        );
    }

    @Test
    void removesManagerAndSuspendsTheOrganization() {
        givenOrganization(activeOrganization());

        Organization result = responsibleUsers.removeManager(
            ORGANIZATION_REFERENCE
        );

        assertEquals(OrganizationStatus.SUSPENDED, result.getStatus());
        OrganizationEntity storedOrganization = findStoredOrganization();
        assertEquals(
            OrganizationStatus.SUSPENDED,
            storedOrganization.getStatus()
        );
        assertNull(storedOrganization.getManagerReference());
    }

    private void givenOrganization(Organization organization) {
        organizationRepository.create(organization);
    }

    private OrganizationEntity findStoredOrganization() {
        return jpaRepository.findByReference(ORGANIZATION_REFERENCE).orElseThrow();
    }

    private static OrganizationResponsibleCommand command(String reference) {
        return new OrganizationResponsibleCommand(
            ORGANIZATION_REFERENCE,
            reference
        );
    }

    private static Organization creatingOrganization() {
        return Organization.create(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            "UTC"
        );
    }

    private static Organization creatingOrganizationWithAdministrator() {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            null,
            OrganizationStatus.CREATING
        );
    }

    private static Organization activeOrganization() {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.ACTIVE
        );
    }
}
