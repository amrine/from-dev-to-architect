package io.teampulse.administration.infrastructure.web;

import io.teampulse.administration.application.error.PlatformAdministrationErrorCode;
import io.teampulse.administration.application.error.PlatformAdministrationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlatformAdministrationHttpErrorAdviceTest {

    private final PlatformAdministrationHttpErrorAdvice advice = new PlatformAdministrationHttpErrorAdvice();

    @ParameterizedTest
    @CsvSource({
        "ORGANIZATION_NOT_FOUND,404,ORGANIZATION_NOT_FOUND,Organization was not found",
        "ORGANIZATION_LOOKUP_UNAVAILABLE,503,ORGANIZATION_LOOKUP_UNAVAILABLE,Organization could not be checked",
        "USER_DATA_INVALID,422,INVALID_USER_DATA,User data is invalid",
        "USER_EMAIL_ALREADY_USED,409,USER_EMAIL_ALREADY_USED,Email is already used in this organization",
        "USER_NOT_FOUND,404,USER_NOT_FOUND,User was not found",
        "USER_TRANSITION_NOT_ALLOWED,409,USER_TRANSITION_NOT_ALLOWED,User transition is not allowed",
        "USER_HAS_ACTIVE_RESPONSIBILITIES,409,USER_HAS_ACTIVE_RESPONSIBILITIES,User has active responsibilities",
        "RESPONSIBILITY_CHECK_UNAVAILABLE,503,RESPONSIBILITY_CHECK_UNAVAILABLE,User responsibilities could not be checked",
        "USER_LIFECYCLE_FAILED,500,USER_LIFECYCLE_FAILED,User lifecycle operation could not be completed"
    })
    void mapsAdministrationFailuresToSanitizedHttpErrors(
        PlatformAdministrationErrorCode errorCode,
        int status,
        String responseCode,
        String publicMessage
    ) {
        IllegalStateException cause = new IllegalStateException("private database diagnostic");
        PlatformAdministrationException exception = new PlatformAdministrationException(errorCode, cause);

        var response = advice.handlePlatformAdministrationException(exception);

        assertEquals(status, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(responseCode, response.getBody().code());
        assertEquals(publicMessage, response.getBody().message());
        assertNotEquals(cause.getMessage(), response.getBody().message());
        assertSame(cause, exception.getCause());
    }
}
