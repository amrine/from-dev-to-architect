package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationResponsibleUsersServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1309-00000ZA7B900";
    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1309-00000ZA7B901";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1309-00000ZA7B902";
    private static final String OTHER_ADMINISTRATOR_REFERENCE =
        "USR-2026-1309-00000ZA7B903";
    private static final String OTHER_MANAGER_REFERENCE =
        "USR-2026-1309-00000ZA7B904";

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationResponsibleUsersValidator responsibleUsersValidator;
    @InjectMocks
    private OrganizationResponsibleUsersService service;

    @Nested
    class AssignmentTests {

        @Test
        void assignsAnAvailableAdministratorAndPersistsOnce() {
            Organization organization = Organization.create(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC"
            );
            givenOrganization(organization);
            when(organizationRepository.update(organization))
                .thenReturn(organization);
            when(responsibleUsersValidator.validateAssignableAdministrator(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);

            Organization result = service.assignAdministrator(command(
                ADMINISTRATOR_REFERENCE
            ));

            assertSame(organization, result);
            assertEquals(
                ADMINISTRATOR_REFERENCE,
                organization.getAdminReference()
            );
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
            verify(organizationRepository).update(organization);
            verify(responsibleUsersValidator)
                .validateAssignableAdministrator(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE
                );
            verify(responsibleUsersValidator, never())
                .areResponsibleUsersAvailable(any(), any(), any());
        }

        @Test
        void assignsAnAvailableAdministratorAndActivatesWhenTheManagerIsAlreadyAvailable() {
            Organization organization = Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                null,
                MANAGER_REFERENCE,
                OrganizationStatus.CREATING
            );
            givenOrganization(organization);
            when(responsibleUsersValidator.validateAssignableAdministrator(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);
            when(responsibleUsersValidator.areResponsibleUsersAvailable(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(true);

            service.assignAdministrator(command(ADMINISTRATOR_REFERENCE));

            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            verify(responsibleUsersValidator)
                .areResponsibleUsersAvailable(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                );
            verify(organizationRepository).update(organization);
        }

        @Test
        void assignsAnAvailableManagerAndActivatesWhenBothAreAvailable() {
            Organization organization = creatingOrganizationWithAdministrator();
            givenOrganization(organization);
            when(responsibleUsersValidator.validateAssignableManager(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);
            when(responsibleUsersValidator.areResponsibleUsersAvailable(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(true);

            service.assignManager(command(MANAGER_REFERENCE));

            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            verify(responsibleUsersValidator)
                .areResponsibleUsersAvailable(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                );
            verify(organizationRepository).update(organization);
        }

        @Test
        void assignsAPendingManagerWithoutChangingTheStatus() {
            Organization organization = creatingOrganizationWithAdministrator();
            givenOrganization(organization);
            when(responsibleUsersValidator.validateAssignableManager(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(UserAvailability.PENDING);

            service.assignManager(command(MANAGER_REFERENCE));

            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            verify(responsibleUsersValidator, never())
                .areResponsibleUsersAvailable(any(), any(), any());
            verify(organizationRepository).update(organization);
        }

        @Test
        void keepsTheOrganizationCreatingWhenTheExistingAdministratorIsPending() {
            Organization organization = creatingOrganizationWithAdministrator();
            givenOrganization(organization);
            when(responsibleUsersValidator.validateAssignableManager(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);
            when(responsibleUsersValidator.areResponsibleUsersAvailable(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(false);

            service.assignManager(command(MANAGER_REFERENCE));

            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
            verify(responsibleUsersValidator)
                .areResponsibleUsersAvailable(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                );
            verify(organizationRepository).update(organization);
        }

        @Test
        void doesNotPersistWhenTheAdministratorCannotBeAssigned() {
            Organization organization = Organization.create(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC"
            );
            givenOrganization(organization);
            OrganizationException validationFailure = new OrganizationException(
                OrganizationErrorCode.ADMINISTRATOR_NOT_FOUND,
                "Organization administrator was not found"
            );
            when(responsibleUsersValidator.validateAssignableAdministrator(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenThrow(validationFailure);

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.assignAdministrator(command(ADMINISTRATOR_REFERENCE))
            );

            assertSame(validationFailure, exception);
            assertNull(organization.getAdminReference());
            verify(organizationRepository, never()).update(any(Organization.class));
        }

        @Test
        void assignsAnAvailableManagerToASuspendedOrganizationAndReactivatesIt() {
            Organization organization = suspendedOrganizationWithoutManager();
            givenOrganization(organization);
            when(responsibleUsersValidator.validateAssignableManager(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);
            when(responsibleUsersValidator.areResponsibleUsersAvailable(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(true);

            service.assignManager(command(MANAGER_REFERENCE));

            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            verify(organizationRepository).update(organization);
        }

        @Test
        void doesNotPersistWhenTheOrganizationDoesNotExist() {
            when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
                .thenReturn(Optional.empty());

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.assignManager(command(MANAGER_REFERENCE))
            );

            assertEquals(OrganizationErrorCode.NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(responsibleUsersValidator);
            verify(organizationRepository, never()).update(any(Organization.class));
        }
    }

    @Nested
    class ReplacementTests {

        @Test
        void replacesAnAvailableAdministratorWithoutChangingTheStatus() {
            Organization organization = activeOrganization();
            givenOrganization(organization);

            service.replaceAdministrator(command(OTHER_ADMINISTRATOR_REFERENCE));

            assertEquals(
                OTHER_ADMINISTRATOR_REFERENCE,
                organization.getAdminReference()
            );
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            verify(organizationRepository).update(organization);
        }

        @Test
        void treatsAdministratorReplacementWithTheSameReferenceAsANoOp() {
            Organization organization = activeOrganization();
            givenOrganization(organization);

            Organization result = service.replaceAdministrator(command(
                ADMINISTRATOR_REFERENCE
            ));

            assertSame(organization, result);
            verify(responsibleUsersValidator, never())
                .validateOperationalAdministrator(any(), any());
            verify(organizationRepository, never()).update(any(Organization.class));
        }

        @Test
        void replacesAnAvailableManagerWithoutChangingTheStatus() {
            Organization organization = activeOrganization();
            givenOrganization(organization);

            service.replaceManager(command(OTHER_MANAGER_REFERENCE));

            assertEquals(OTHER_MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            verify(organizationRepository).update(organization);
        }

        @Test
        void treatsManagerReplacementWithTheSameReferenceAsANoOp() {
            Organization organization = activeOrganization();
            givenOrganization(organization);

            Organization result = service.replaceManager(command(
                MANAGER_REFERENCE
            ));

            assertSame(organization, result);
            verify(responsibleUsersValidator, never())
                .validateOperationalManager(any(), any());
            verify(organizationRepository, never()).update(any(Organization.class));
        }
    }

    @Nested
    class RemovalTests {

        @Test
        void removesManagerAndSuspendsAnActiveOrganization() {
            Organization organization = activeOrganization();
            givenOrganization(organization);

            service.removeManager(ORGANIZATION_REFERENCE);

            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
            assertNull(organization.getManagerReference());
            verifyNoInteractions(responsibleUsersValidator);
            verify(organizationRepository).update(organization);
        }

        @Test
        void removesMissingManagerFromSuspendedOrganization() {
            Organization organization = suspendedOrganizationWithoutManager();
            givenOrganization(organization);

            service.removeManager(ORGANIZATION_REFERENCE);

            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
            assertNull(organization.getManagerReference());
            verify(organizationRepository).update(organization);
        }
    }

    private void givenOrganization(Organization organization) {
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
            .thenReturn(Optional.of(organization));
    }

    private static OrganizationResponsibleCommand command(String reference) {
        return new OrganizationResponsibleCommand(
            ORGANIZATION_REFERENCE,
            reference
        );
    }

    private static Organization creatingOrganizationWithAdministrator() {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "Team Pulse",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            null,
            OrganizationStatus.CREATING
        );
    }

    private static Organization activeOrganization() {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "Team Pulse",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.ACTIVE
        );
    }

    private static Organization suspendedOrganizationWithoutManager() {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "Team Pulse",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            null,
            OrganizationStatus.SUSPENDED
        );
    }
}
