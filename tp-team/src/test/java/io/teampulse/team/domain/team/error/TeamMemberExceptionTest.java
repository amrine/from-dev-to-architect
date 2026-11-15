package io.teampulse.team.domain.team.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamMemberExceptionTest {

    @Test
    void rejectsNullErrorCode() {
        assertThrows(NullPointerException.class, () -> new TeamMemberException(null, "Membership failure"));
    }

    @Test
    void rejectsNullMessage() {
        assertThrows(NullPointerException.class, () -> new TeamMemberException(TeamMemberErrorCode.NOT_FOUND, null));
    }

    @Test
    void returnsErrorCodeWithoutCause() {
        TeamMemberException exception =
                new TeamMemberException(TeamMemberErrorCode.NOT_FOUND, "Team member was not found");

        assertEquals(TeamMemberErrorCode.NOT_FOUND, exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    void preservesCause() {
        IllegalStateException cause = new IllegalStateException("Directory failure");

        TeamMemberException exception = new TeamMemberException(
                TeamMemberErrorCode.USER_DIRECTORY_UNAVAILABLE, "Unable to validate team member user", cause);

        assertSame(cause, exception.getCause());
    }
}
