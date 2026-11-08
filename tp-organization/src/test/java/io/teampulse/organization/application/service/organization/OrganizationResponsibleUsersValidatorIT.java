package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.organization.AbstractOrganizationJpaIT;
import io.teampulse.organization.fixture.DeterministicUserDirectory;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationResponsibleUsersValidatorIT extends AbstractOrganizationJpaIT {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1409-00000ZA7B920";
    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1409-00000ZA7B920";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1409-00000ZA7B921";

    @Inject
    private OrganizationResponsibleUsersValidator validator;

    @Inject
    private DeterministicUserDirectory userDirectory;

    @BeforeEach
    void resetUserDirectory() {
        userDirectory.reset();
    }

    @Test
    void rejectsPendingAssignableAdministrator() {
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, UserAvailability.PENDING
        );

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> validator.validateAssignableAdministrator(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )
        );
        assertEquals(OrganizationErrorCode.ADMINISTRATOR_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void rejectsUnavailableOperationalManager() {
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, MANAGER_REFERENCE, UserAvailability.PENDING
        );

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
        userDirectory.setAvailability(
            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, UserAvailability.NOT_FOUND
        );

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

}
