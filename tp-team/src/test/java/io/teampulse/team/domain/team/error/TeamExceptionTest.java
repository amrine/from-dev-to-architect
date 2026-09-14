package io.teampulse.team.domain.team.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamExceptionTest {

    @Test
    void rejectsNullErrorCode() {
        assertThrows(NullPointerException.class, () -> new TeamException(null, "Team failure"));
    }

    @Test
    void rejectsNullMessage() {
        assertThrows(NullPointerException.class, () -> new TeamException(TeamErrorCode.INVALID_NAME, null));
    }

    @Test
    void returnsErrorCodeWithoutCause() {
        TeamException exception = new TeamException(TeamErrorCode.INVALID_NAME, "Invalid team name");

        assertEquals(TeamErrorCode.INVALID_NAME, exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    void preservesCause() {
        IllegalStateException cause = new IllegalStateException("Directory failure");

        TeamException exception = new TeamException(
                TeamErrorCode.USER_DIRECTORY_UNAVAILABLE, "Unable to validate responsible user", cause);

        assertSame(cause, exception.getCause());
    }
}
