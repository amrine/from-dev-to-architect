package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.organization.api.lifecycle.OrganizationLifecycleErrorCode;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleException;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OrganizationHttpErrorAdviceTest {

    private final OrganizationHttpErrorAdvice advice = new OrganizationHttpErrorAdvice();

    @ParameterizedTest
    @CsvSource({
        "ORGANIZATION_NOT_FOUND,404,ORGANIZATION_NOT_FOUND,Organization was not found",
        "INVALID_NAME,422,INVALID_ORGANIZATION_NAME,Organization name is invalid",
        "INVALID_TIMEZONE,422,INVALID_ORGANIZATION_TIMEZONE,Organization timezone is invalid",
        "TRANSITION_NOT_ALLOWED,409,ORGANIZATION_TRANSITION_NOT_ALLOWED,Organization transition is not allowed",
        "CONCURRENT_MODIFICATION,409,ORGANIZATION_CONCURRENT_MODIFICATION,Organization was modified concurrently",
        "REFERENCE_GENERATION_FAILED,500,ORGANIZATION_REFERENCE_UNAVAILABLE,Organization could not be created",
        "OPERATION_FAILED,500,ORGANIZATION_OPERATION_FAILED,Organization lifecycle operation failed"
    })
    void mapsPublicFailuresWithoutLeakingTheirCause(
        OrganizationLifecycleErrorCode category, int status, String code, String message
    ) {
        var cause = new IllegalStateException("private database diagnostic");
        var failure = new OrganizationLifecycleException(category, cause);
        var response = advice.handleOrganizationLifecycleException(failure);
        assertEquals(status, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(code, response.getBody().code());
        assertEquals(message, response.getBody().message());
        assertSame(cause, failure.getCause());
    }

    @ParameterizedTest
    @CsvSource({
        "NOT_FOUND,404,ORGANIZATION_NOT_FOUND,Organization was not found",
        "INVALID_NAME,422,INVALID_ORGANIZATION_NAME,Organization name is invalid",
        "INVALID_TIMEZONE,422,INVALID_ORGANIZATION_TIMEZONE,Organization timezone is invalid",
        "INVALID_STATUS_TRANSITION,409,ORGANIZATION_TRANSITION_NOT_ALLOWED,Organization transition is not allowed",
        "ACTIVATION_REQUIREMENTS_NOT_MET,409,ORGANIZATION_ACTIVATION_REQUIREMENTS_NOT_MET,Organization activation requirements are not met",
        "LIFECYCLE_TRANSITION_DEFERRED,409,ORGANIZATION_LIFECYCLE_DEFERRED,Completing organization responsibilities is deferred",
        "ADMINISTRATOR_NOT_FOUND,404,ORGANIZATION_ADMINISTRATOR_NOT_FOUND,Organization administrator was not found",
        "ADMINISTRATOR_NOT_AVAILABLE,409,ORGANIZATION_ADMINISTRATOR_NOT_AVAILABLE,Organization administrator is not available",
        "MANAGER_NOT_FOUND,404,ORGANIZATION_MANAGER_NOT_FOUND,Organization manager was not found",
        "MANAGER_NOT_AVAILABLE,409,ORGANIZATION_MANAGER_NOT_AVAILABLE,Organization manager is not available",
        "USER_DIRECTORY_UNAVAILABLE,503,ORGANIZATION_USER_DIRECTORY_UNAVAILABLE,Organization responsibilities could not be validated",
        "REFERENCE_GENERATION_FAILED,500,ORGANIZATION_REFERENCE_UNAVAILABLE,Organization could not be created",
        "CONCURRENT_MODIFICATION,409,ORGANIZATION_CONCURRENT_MODIFICATION,Organization was modified concurrently"
    })
    void mapsInternalFailuresWithoutExposingDiagnostics(
        OrganizationErrorCode category, int status, String code, String message
    ) {
        var response = advice.handleOrganizationException(new OrganizationException(
            category, "private user and database diagnostic", new IllegalStateException("private cause")
        ));
        assertEquals(status, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(code, response.getBody().code());
        assertEquals(message, response.getBody().message());
    }
}
