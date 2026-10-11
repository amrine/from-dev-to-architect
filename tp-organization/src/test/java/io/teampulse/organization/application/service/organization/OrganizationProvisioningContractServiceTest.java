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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void mapsInvalidTimezoneWithoutExposingInternalMessageOrCause() {
        OrganizationProvisioningCommand publicCommand =
            new OrganizationProvisioningCommand("Acme", "private-invalid-zone");
        when(organizationLifecycleUseCase.create(
            new CreateOrganizationCommand("Acme", "private-invalid-zone")
        )).thenThrow(new OrganizationException(
            OrganizationErrorCode.INVALID_TIMEZONE,
            "Unknown timezone private-invalid-zone"
        ));

        OrganizationProvisioningException exception = assertThrows(
            OrganizationProvisioningException.class,
            () -> service.createOrganization(publicCommand)
        );

        assertEquals(OrganizationProvisioningErrorCode.INVALID_TIMEZONE, exception.getErrorCode());
        assertEquals("Organization timezone is invalid", exception.getMessage());
        assertNull(exception.getCause());
    }
}
