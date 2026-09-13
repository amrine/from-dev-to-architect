package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.organization.AbstractIntegrationTest;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationResponsibleUsersValidatorIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1409-00000ZA7B920";
    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1409-00000ZA7B920";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1409-00000ZA7B921";

    @Inject
    private OrganizationResponsibleUsersValidator validator;

    @MockitoBean
    private UserDirectory userDirectory;

    @Test
    void acceptsPendingAssignableAdministrator() {
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE
        )).thenReturn(UserAvailability.PENDING);

        assertEquals(
            UserAvailability.PENDING,
            validator.validateAssignableAdministrator(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )
        );
    }

    @Test
    void rejectsUnavailableOperationalManager() {
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            MANAGER_REFERENCE
        )).thenReturn(UserAvailability.PENDING);

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> validator.validateOperationalManager(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )
        );

        assertEquals(
            OrganizationErrorCode.MANAGER_NOT_AVAILABLE,
            exception.getErrorCode()
        );
    }

    @Test
    void mapsMissingAdministratorToTheRoleSpecificError() {
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE
        )).thenReturn(UserAvailability.NOT_FOUND);

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> validator.validateOperationalAdministrator(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )
        );

        assertEquals(
            OrganizationErrorCode.ADMINISTRATOR_NOT_FOUND,
            exception.getErrorCode()
        );
    }

    @Test
    void translatesDirectoryFailureAndPreservesItsCause() {
        RuntimeException cause = new IllegalStateException("identity down");
        UserDirectoryException directoryFailure =
            new UserDirectoryException(cause);
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE
        )).thenThrow(directoryFailure);

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> validator.validateOperationalAdministrator(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )
        );

        assertEquals(
            OrganizationErrorCode.USER_DIRECTORY_UNAVAILABLE,
            exception.getErrorCode()
        );
        assertSame(directoryFailure, exception.getCause());
    }

    @Test
    void checksSharedResponsibleReferenceOnlyOnce() {
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE
        )).thenReturn(UserAvailability.AVAILABLE);

        validator.validateOperationalResponsibleUsers(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE,
            ADMINISTRATOR_REFERENCE
        );

        verify(userDirectory, times(1)).check(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE
        );
        assertTrue(
            validator.areResponsibleUsersAvailable(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )
        );
    }
}
