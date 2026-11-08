package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.organization.AbstractOrganizationJpaIT;
import io.teampulse.organization.fixture.DeterministicUserDirectory;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleUsersUseCase;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationResponsibleUsersServiceIT extends AbstractOrganizationJpaIT {

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

    @Inject
    private DeterministicUserDirectory userDirectory;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaRepository::deleteAllInBatch);
        userDirectory.reset();
    }

    @Test
    void refusesTheFinalAvailableAssignmentAndPersistsNoActiveTransition() {
        givenOrganization(creatingOrganization());
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, UserAvailability.AVAILABLE
        );
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, MANAGER_REFERENCE, UserAvailability.AVAILABLE
        );

        responsibleUsers.assignAdministrator(command(ADMINISTRATOR_REFERENCE));
        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> responsibleUsers.assignManager(command(MANAGER_REFERENCE))
        );

        assertEquals(
            OrganizationErrorCode.LIFECYCLE_TRANSITION_DEFERRED,
            exception.getErrorCode()
        );
        assertEquals(OrganizationStatus.CREATING, findStoredOrganization().getStatus());
        assertEquals(ADMINISTRATOR_REFERENCE, findStoredOrganization().getAdminReference());
        assertNull(findStoredOrganization().getManagerReference());
    }

    @Test
    void refusesToReactivateASuspendedOrganizationByAssigningItsMissingManager() {
        Organization suspended = Organization.restore(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            null,
            OrganizationStatus.SUSPENDED
        );
        inTransactionTemplate(() -> {
            jpaRepository.deleteAllInBatch();
            organizationRepository.create(suspended);
        });
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, UserAvailability.AVAILABLE
        );
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, MANAGER_REFERENCE, UserAvailability.AVAILABLE
        );

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> responsibleUsers.assignManager(command(MANAGER_REFERENCE))
        );

        assertEquals(
            OrganizationErrorCode.LIFECYCLE_TRANSITION_DEFERRED,
            exception.getErrorCode()
        );
        assertEquals(
            OrganizationStatus.SUSPENDED,
            findStoredOrganization().getStatus()
        );
        assertNull(findStoredOrganization().getManagerReference());
    }

    @Test
    void refusesPendingManagerWithoutChangingThePersistedOrganization() {
        givenOrganization(creatingOrganizationWithAdministrator());
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, MANAGER_REFERENCE, UserAvailability.PENDING
        );
        OrganizationEntity before = findStoredOrganization();

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> responsibleUsers.assignManager(command(MANAGER_REFERENCE))
        );

        assertEquals(OrganizationErrorCode.MANAGER_NOT_AVAILABLE, exception.getErrorCode());
        OrganizationEntity after = findStoredOrganization();
        assertEquals(before.getVersion(), after.getVersion());
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getAdminReference(), after.getAdminReference());
        assertEquals(before.getManagerReference(), after.getManagerReference());
    }

    @Test
    void replacesManagerWithoutChangingTheOrganizationStatus() {
        givenOrganization(activeOrganization());
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, OTHER_MANAGER_REFERENCE, UserAvailability.AVAILABLE
        );

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
