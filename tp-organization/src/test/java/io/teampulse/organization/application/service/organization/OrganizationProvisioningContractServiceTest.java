package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationProvisioningErrorCode;
import io.teampulse.organization.api.organization.OrganizationProvisioningException;
import io.teampulse.organization.api.organization.OrganizationProvisioningResult;
import io.teampulse.organization.application.port.in.organization.CreateOrganizationCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationLifecycleUseCase;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationProvisioningContractServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1409-00000ZA7B950";

    @Mock
    private OrganizationLifecycleUseCase organizationLifecycleUseCase;

    @InjectMocks
    private OrganizationProvisioningContractService service;

    @Test
    void createsAnOrganizationThroughThePublicContractAndReturnsItsReference() {
        OrganizationProvisioningCommand publicCommand =
            new OrganizationProvisioningCommand("Acme", "UTC");
        when(organizationLifecycleUseCase.create(
            new CreateOrganizationCommand("Acme", "UTC")
        )).thenReturn(Organization.create(ORGANIZATION_REFERENCE, "Acme", "UTC"));

        OrganizationProvisioningResult result =
            service.createOrganization(publicCommand);

        assertEquals(ORGANIZATION_REFERENCE, result.organizationReference());
        assertEquals(OrganizationLifecycleState.CREATING, result.status());
        verify(organizationLifecycleUseCase).create(
            new CreateOrganizationCommand("Acme", "UTC")
        );
    }

    @Test
    void mapsInvalidTimezoneWithoutExposingInternalMessageAndPreservesTheCause() {
        OrganizationProvisioningCommand publicCommand =
            new OrganizationProvisioningCommand("Acme", "private-invalid-zone");
        OrganizationException internalFailure = new OrganizationException(
            OrganizationErrorCode.INVALID_TIMEZONE,
            "Unknown timezone private-invalid-zone"
        );
        when(organizationLifecycleUseCase.create(
            new CreateOrganizationCommand("Acme", "private-invalid-zone")
        )).thenThrow(internalFailure);

        OrganizationProvisioningException exception = assertThrows(
            OrganizationProvisioningException.class,
            () -> service.createOrganization(publicCommand)
        );

        assertEquals(OrganizationProvisioningErrorCode.INVALID_TIMEZONE, exception.getErrorCode());
        assertEquals("Organization timezone is invalid", exception.getMessage());
        assertSame(internalFailure, exception.getCause());
    }

    @Test
    void mapsPersistenceFailuresToThePublicTechnicalCategoryAndPreservesTheCause() {
        DataAccessResourceFailureException persistenceFailure =
            new DataAccessResourceFailureException("Database is unavailable");
        when(organizationLifecycleUseCase.create(
            new CreateOrganizationCommand("Acme", "UTC")
        )).thenThrow(persistenceFailure);

        OrganizationProvisioningException exception = assertThrows(
            OrganizationProvisioningException.class,
            () -> service.createOrganization(
                new OrganizationProvisioningCommand("Acme", "UTC")
            )
        );

        assertEquals(
            OrganizationProvisioningErrorCode.OPERATION_FAILED,
            exception.getErrorCode()
        );
        assertEquals("Organization provisioning failed", exception.getMessage());
        assertSame(persistenceFailure, exception.getCause());
    }

    @Test
    void preservesTheCauseChainWhenMappingAnInternalFailure() {
        IllegalStateException persistenceCause = new IllegalStateException("database conflict");
        OrganizationException internalFailure = new OrganizationException(
            OrganizationErrorCode.CONCURRENT_MODIFICATION,
            "Organization was modified concurrently",
            persistenceCause
        );
        when(organizationLifecycleUseCase.create(
            new CreateOrganizationCommand("Acme", "UTC")
        )).thenThrow(internalFailure);

        OrganizationProvisioningException exception = assertThrows(
            OrganizationProvisioningException.class,
            () -> service.createOrganization(
                new OrganizationProvisioningCommand("Acme", "UTC")
            )
        );

        assertEquals(
            OrganizationProvisioningErrorCode.CONCURRENT_MODIFICATION,
            exception.getErrorCode()
        );
        assertSame(internalFailure, exception.getCause());
        assertSame(persistenceCause, exception.getCause().getCause());
    }
}
