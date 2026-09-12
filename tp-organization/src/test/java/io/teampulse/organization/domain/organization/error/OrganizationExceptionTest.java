package io.teampulse.organization.domain.organization.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationExceptionTest {

    @Test
    void rejectsNullErrorCode() {
        assertThrows(
            NullPointerException.class,
            () -> new OrganizationException(null, "Organization failure")
        );
    }

    @Test
    void rejectsNullMessage() {
        assertThrows(
            NullPointerException.class,
            () -> new OrganizationException(OrganizationErrorCode.INVALID_NAME, null)
        );
    }

    @Test
    void returnsErrorCodeWithoutCause() {
        OrganizationException exception = new OrganizationException(
            OrganizationErrorCode.INVALID_NAME,
            "Invalid organization name"
        );

        assertEquals(OrganizationErrorCode.INVALID_NAME, exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    void preservesCause() {
        IllegalStateException cause = new IllegalStateException("Directory failure");

        OrganizationException exception = new OrganizationException(
            OrganizationErrorCode.USER_DIRECTORY_UNAVAILABLE,
            "Unable to validate responsible user",
            cause
        );

        assertSame(cause, exception.getCause());
    }
}
