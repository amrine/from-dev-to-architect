package io.teampulse.organization.application.service.organization;

import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.organization.application.port.in.organization.CreateOrganizationCommand;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationLifecycleServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1309-00000ZA7B900";

    @Mock
    private ReferenceFactory referenceFactory;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationResponsibleUsersValidator responsibleUsersValidator;
    @InjectMocks
    private OrganizationLifecycleService service;

    @Nested
    class CreationTests {

        @Test
        void createsAndPersistsAnOrganizationInOrder() {
            prepareSuccessfulCreation();
            ArgumentCaptor<Organization> organizationCaptor =
                ArgumentCaptor.forClass(Organization.class);

            service.create(validCommand());

            InOrder orderedInteractions = inOrder(
                referenceFactory,
                organizationRepository
            );
            orderedInteractions.verify(referenceFactory).generate("ORG");
            orderedInteractions.verify(organizationRepository)
                .create(organizationCaptor.capture());

            Organization createdOrganization = organizationCaptor.getValue();
            assertEquals(
                ORGANIZATION_REFERENCE,
                createdOrganization.getReference()
            );
            assertEquals("Team Pulse", createdOrganization.getName());
            assertEquals(
                ZoneId.of("Europe/Paris"),
                createdOrganization.getTimezone()
            );
            assertNull(createdOrganization.getAdminReference());
            assertNull(createdOrganization.getManagerReference());
            assertEquals(
                OrganizationStatus.CREATING,
                createdOrganization.getStatus()
            );
        }

        @Test
        void returnsThePersistedOrganization() {
            when(referenceFactory.generate("ORG"))
                .thenReturn(ORGANIZATION_REFERENCE);
            Organization persistedOrganization = Organization.create(
                ORGANIZATION_REFERENCE,
                "Persisted organization",
                "UTC"
            );
            when(organizationRepository.create(any(Organization.class)))
                .thenReturn(persistedOrganization);

            Organization result = service.create(validCommand());

            assertSame(persistedOrganization, result);
        }

        @Test
        void doesNotPersistWhenReferenceGenerationFails() {
            IllegalStateException generationFailure =
                new IllegalStateException("Reference generation failed");
            when(referenceFactory.generate("ORG")).thenThrow(generationFailure);

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.create(validCommand())
            );

            assertSame(generationFailure, exception);
            verifyNoInteractions(organizationRepository);
        }

        @Test
        void doesNotPersistWhenDomainValidationFails() {
            when(referenceFactory.generate("ORG"))
                .thenReturn(ORGANIZATION_REFERENCE);

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.create(
                    new CreateOrganizationCommand(" ", "Europe/Paris")
                )
            );

            assertEquals(
                OrganizationErrorCode.INVALID_NAME,
                exception.getErrorCode()
            );
            verify(organizationRepository, never())
                .create(any(Organization.class));
        }

        @Test
        void propagatesReferenceCollisionWithoutRetry() {
            when(referenceFactory.generate("ORG"))
                .thenReturn(ORGANIZATION_REFERENCE);
            OrganizationException collision = new OrganizationException(
                OrganizationErrorCode.REFERENCE_GENERATION_FAILED,
                "Organization reference already exists"
            );
            when(organizationRepository.create(any(Organization.class)))
                .thenThrow(collision);

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.create(validCommand())
            );

            assertSame(collision, exception);
            verify(referenceFactory).generate("ORG");
            verify(organizationRepository).create(any(Organization.class));
        }

        private void prepareSuccessfulCreation() {
            when(referenceFactory.generate("ORG"))
                .thenReturn(ORGANIZATION_REFERENCE);
            when(organizationRepository.create(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        }
    }

    @Nested
    class LifecycleOperationsTests {

        @Test
        void activatesCreatingOrganizationAfterOperationalValidation() {
            Organization organization = Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                OrganizationStatus.CREATING
            );
            givenOrganization(organization);
            when(organizationRepository.update(organization))
                .thenReturn(organization);

            Organization result = service.activate(ORGANIZATION_REFERENCE);

            assertSame(organization, result);
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            verify(responsibleUsersValidator)
                .validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                );
            verify(organizationRepository).update(organization);
        }

        @Test
        void rejectsActivationWhenOrganizationIsMissing() {
            when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
                .thenReturn(Optional.empty());

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.activate(ORGANIZATION_REFERENCE)
            );

            assertEquals(OrganizationErrorCode.NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(responsibleUsersValidator);
            verify(organizationRepository, never()).update(any(Organization.class));
        }

        @Test
        void rejectsActivationWhenResponsibleReferencesAreMissing() {
            Organization organization = Organization.create(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC"
            );
            givenOrganization(organization);

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.activate(ORGANIZATION_REFERENCE)
            );

            assertEquals(
                OrganizationErrorCode.ACTIVATION_REQUIREMENTS_NOT_MET,
                exception.getErrorCode()
            );
            verifyNoInteractions(responsibleUsersValidator);
            verify(organizationRepository, never()).update(any(Organization.class));
        }

        @Test
        void doesNotPersistWhenAResponsibleUserIsNotOperational() {
            Organization organization = Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                OrganizationStatus.CREATING
            );
            givenOrganization(organization);
            OrganizationException validationFailure = new OrganizationException(
                OrganizationErrorCode.MANAGER_NOT_AVAILABLE,
                "Organization manager is not available"
            );
            org.mockito.Mockito.doThrow(validationFailure)
                .when(responsibleUsersValidator)
                .validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                );

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.activate(ORGANIZATION_REFERENCE)
            );

            assertSame(validationFailure, exception);
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
            verify(organizationRepository, never()).update(any(Organization.class));
        }

        @Test
        void suspendsActiveOrganizationAndPreservesResponsibleReferences() {
            Organization organization = organizationInStatus(OrganizationStatus.ACTIVE);
            givenOrganization(organization);

            service.suspend(ORGANIZATION_REFERENCE);

            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
            assertEquals(ADMINISTRATOR_REFERENCE, organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            verifyNoInteractions(responsibleUsersValidator);
            verify(organizationRepository).update(organization);
        }

        @Test
        void reactivatesSuspendedOrganizationAfterOperationalValidation() {
            Organization organization = organizationInStatus(OrganizationStatus.SUSPENDED);
            givenOrganization(organization);
            when(organizationRepository.update(organization))
                .thenReturn(organization);

            Organization result = service.reactivate(ORGANIZATION_REFERENCE);

            assertSame(organization, result);
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            verify(responsibleUsersValidator)
                .validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                );
            verify(organizationRepository).update(organization);
        }

        @Test
        void archivesOrganizationWithoutCheckingResponsibleAvailability() {
            Organization organization = organizationInStatus(OrganizationStatus.ACTIVE);
            givenOrganization(organization);

            service.archive(ORGANIZATION_REFERENCE);

            assertEquals(OrganizationStatus.ARCHIVED, organization.getStatus());
            verifyNoInteractions(responsibleUsersValidator);
            verify(organizationRepository).update(organization);
        }

        @Test
        void rejectsReactivationWhenManagerReferenceIsMissing() {
            Organization organization = Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                ADMINISTRATOR_REFERENCE,
                null,
                OrganizationStatus.SUSPENDED
            );
            givenOrganization(organization);

            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> service.reactivate(ORGANIZATION_REFERENCE)
            );

            assertEquals(
                OrganizationErrorCode.ACTIVATION_REQUIREMENTS_NOT_MET,
                exception.getErrorCode()
            );
            verifyNoInteractions(responsibleUsersValidator);
            verify(organizationRepository, never()).update(any(Organization.class));
        }

        private void givenOrganization(Organization organization) {
            when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
                .thenReturn(Optional.of(organization));
        }

        private Organization organizationInStatus(OrganizationStatus status) {
            return Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                ADMINISTRATOR_REFERENCE,
                MANAGER_REFERENCE,
                status
            );
        }
    }

    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1309-00000ZA7B901";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1309-00000ZA7B902";

    private static CreateOrganizationCommand validCommand() {
        return new CreateOrganizationCommand(
            " Team Pulse ",
            "Europe/Paris"
        );
    }
}
