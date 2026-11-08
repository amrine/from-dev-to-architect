package io.teampulse.organization.application.service.organization;

import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleErrorCode;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleException;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleResult;
import io.teampulse.organization.api.lifecycle.OrganizationProvisioningCommand;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B900";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-1309-00000ZA7B901";
    private static final String MANAGER_REFERENCE = "USR-2026-1309-00000ZA7B902";

    @Mock
    private ReferenceFactory referenceFactory;
    @Mock
    private OrganizationRepository organizationRepository;
    @InjectMocks
    private OrganizationLifecycleService service;

    @Test
    void createsAndReturnsThePersistedOrganizationThroughThePublicContract() {
        when(referenceFactory.generate("ORG")).thenReturn(ORGANIZATION_REFERENCE);
        when(organizationRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationLifecycleResult result = service.create(new OrganizationProvisioningCommand(
            " Team Pulse ", "Europe/Paris"
        ));

        var organizationCaptor = ArgumentCaptor.forClass(Organization.class);
        var order = inOrder(referenceFactory, organizationRepository);
        order.verify(referenceFactory).generate("ORG");
        order.verify(organizationRepository).create(organizationCaptor.capture());
        Organization created = organizationCaptor.getValue();
        assertEquals(OrganizationStatus.CREATING, created.getStatus());
        assertEquals(ZoneId.of("Europe/Paris"), created.getTimezone());
        assertNull(created.getAdminReference());
        assertNull(created.getManagerReference());
        assertEquals(new OrganizationLifecycleResult(
            ORGANIZATION_REFERENCE, "Team Pulse", "Europe/Paris", "CREATING", null, null
        ), result);
    }

    @ParameterizedTest
    @MethodSource("invalidData")
    void mapsInvalidDataWithoutPersisting(OrganizationProvisioningCommand command,
                                         OrganizationLifecycleErrorCode expectedCode) {
        when(referenceFactory.generate("ORG")).thenReturn(ORGANIZATION_REFERENCE);
        OrganizationLifecycleException failure = assertThrows(OrganizationLifecycleException.class,
            () -> service.create(command));
        assertEquals(expectedCode, failure.getErrorCode());
        assertEquals(expectedCode.getMessage(), failure.getMessage());
        assertInstanceOf(OrganizationException.class, failure.getCause());
        verifyNoInteractions(organizationRepository);
    }

    static Stream<Arguments> invalidData() {
        return Stream.of(
            Arguments.of(new OrganizationProvisioningCommand(" ", "UTC"), OrganizationLifecycleErrorCode.INVALID_NAME),
            Arguments.of(new OrganizationProvisioningCommand("Acme", "private-invalid-zone"),
                OrganizationLifecycleErrorCode.INVALID_TIMEZONE)
        );
    }

    @Test
    void wrapsReferenceGenerationFailureAndPreservesItsCause() {
        RuntimeException cause = new IllegalStateException("private generation failure");
        when(referenceFactory.generate("ORG")).thenThrow(cause);
        OrganizationLifecycleException failure = assertThrows(OrganizationLifecycleException.class,
            () -> service.create(new OrganizationProvisioningCommand("Acme", "UTC")));
        assertEquals(OrganizationLifecycleErrorCode.OPERATION_FAILED, failure.getErrorCode());
        assertSame(cause, failure.getCause());
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void wrapsPersistenceFailureAndPreservesItsCause() {
        when(referenceFactory.generate("ORG")).thenReturn(ORGANIZATION_REFERENCE);
        RuntimeException cause = new IllegalStateException("private persistence failure");
        when(organizationRepository.create(any())).thenThrow(cause);
        OrganizationLifecycleException failure = assertThrows(OrganizationLifecycleException.class,
            () -> service.create(new OrganizationProvisioningCommand("Acme", "UTC")));
        assertEquals(OrganizationLifecycleErrorCode.OPERATION_FAILED, failure.getErrorCode());
        assertEquals("Organization lifecycle operation failed", failure.getMessage());
        assertSame(cause, failure.getCause());
    }

    @ParameterizedTest
    @EnumSource(value = OrganizationErrorCode.class, names = {"REFERENCE_GENERATION_FAILED", "CONCURRENT_MODIFICATION"})
    void preservesKnownTechnicalFailureCategoriesWithoutRetry(OrganizationErrorCode code) {
        when(referenceFactory.generate("ORG")).thenReturn(ORGANIZATION_REFERENCE);
        OrganizationException cause = new OrganizationException(code, "private technical details");
        when(organizationRepository.create(any())).thenThrow(cause);
        OrganizationLifecycleException failure = assertThrows(OrganizationLifecycleException.class,
            () -> service.create(new OrganizationProvisioningCommand("Acme", "UTC")));
        assertEquals(code.name(), failure.getErrorCode().name());
        assertSame(cause, failure.getCause());
        verify(referenceFactory).generate("ORG");
        verify(organizationRepository).create(any());
    }

    @Test
    void suspendsAnActiveOrganizationAndPreservesItsResponsibilities() {
        Organization organization = organizationInStatus(OrganizationStatus.ACTIVE);
        givenOrganization(organization);
        OrganizationLifecycleResult result = service.suspend(ORGANIZATION_REFERENCE);
        assertEquals("SUSPENDED", result.status());
        assertEquals(ADMINISTRATOR_REFERENCE, result.administratorReference());
        assertEquals(MANAGER_REFERENCE, result.managerReference());
        verify(organizationRepository).update(organization);
    }

    @Test
    void archivesAnOrganization() {
        Organization organization = organizationInStatus(OrganizationStatus.CREATING);
        givenOrganization(organization);
        assertEquals("ARCHIVED", service.archive(ORGANIZATION_REFERENCE).status());
        verify(organizationRepository).update(organization);
    }

    @Test
    void rejectsAnInvalidTransitionBeforePersisting() {
        Organization organization = organizationInStatus(OrganizationStatus.CREATING);
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE)).thenReturn(Optional.of(organization));
        OrganizationLifecycleException failure = assertThrows(OrganizationLifecycleException.class,
            () -> service.suspend(ORGANIZATION_REFERENCE));
        assertEquals(OrganizationLifecycleErrorCode.TRANSITION_NOT_ALLOWED, failure.getErrorCode());
        assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        verify(organizationRepository, never()).update(any());
    }

    @Test
    void translatesAnAbsentOrganizationWithoutWriting() {
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE)).thenReturn(Optional.empty());
        OrganizationLifecycleException failure = assertThrows(OrganizationLifecycleException.class,
            () -> service.archive(ORGANIZATION_REFERENCE));
        assertEquals(OrganizationLifecycleErrorCode.ORGANIZATION_NOT_FOUND, failure.getErrorCode());
        verify(organizationRepository, never()).update(any());
    }

    @Test
    void propagatesAnAlreadyPublicFailureWithoutWrappingItAgain() {
        var failure = new OrganizationLifecycleException(
            OrganizationLifecycleErrorCode.OPERATION_FAILED, new IllegalStateException("private diagnostic")
        );
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE)).thenThrow(failure);
        assertSame(failure, assertThrows(OrganizationLifecycleException.class,
            () -> service.archive(ORGANIZATION_REFERENCE)));
    }

    @Test
    void translatesUnexpectedInternalCategoriesToTechnicalPublicFailure() {
        var cause = new OrganizationException(OrganizationErrorCode.USER_DIRECTORY_UNAVAILABLE, "private diagnostic");
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE)).thenThrow(cause);
        var failure = assertThrows(OrganizationLifecycleException.class, () -> service.archive(ORGANIZATION_REFERENCE));
        assertEquals(OrganizationLifecycleErrorCode.OPERATION_FAILED, failure.getErrorCode());
        assertSame(cause, failure.getCause());
    }

    private void givenOrganization(Organization organization) {
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE)).thenReturn(Optional.of(organization));
        when(organizationRepository.update(organization)).thenReturn(organization);
    }

    private static Organization organizationInStatus(OrganizationStatus status) {
        return Organization.restore(ORGANIZATION_REFERENCE, "Acme", "UTC",
            ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE, status);
    }
}
