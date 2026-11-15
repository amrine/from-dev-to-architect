package io.teampulse.team.application.service.teammember;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.team.domain.team.error.TeamMemberErrorCode;
import io.teampulse.team.domain.team.error.TeamMemberException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamMemberUserAvailabilityValidatorTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B900";
    private static final String USER_REFERENCE = "USR-2026-1309-00000ZA7B901";

    @Mock
    private UserDirectory userDirectory;

    @InjectMocks
    private TeamMemberUserAvailabilityValidator validator;

    @Nested
    class ValidateAvailable {

        @ParameterizedTest
        @MethodSource("strictAvailabilityMappings")
        void requiresTheUserToBeAvailable(UserAvailability availability, TeamMemberErrorCode expectedErrorCode) {
            when(userDirectory.check(ORGANIZATION_REFERENCE, USER_REFERENCE)).thenReturn(availability);

            assertAvailability(
                    expectedErrorCode, () -> validator.validateAvailable(ORGANIZATION_REFERENCE, USER_REFERENCE));

            verify(userDirectory).check(ORGANIZATION_REFERENCE, USER_REFERENCE);
        }

        @Test
        void preservesTheCauseWhenTheUserDirectoryCannotBeReached() {
            UserDirectoryException directoryException =
                    new UserDirectoryException(new IllegalStateException("Identity unavailable"));
            when(userDirectory.check(ORGANIZATION_REFERENCE, USER_REFERENCE)).thenThrow(directoryException);

            TeamMemberException exception = assertThrows(
                    TeamMemberException.class,
                    () -> validator.validateAvailable(ORGANIZATION_REFERENCE, USER_REFERENCE));

            assertEquals(TeamMemberErrorCode.USER_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
            assertSame(directoryException, exception.getCause());
        }

        private static Stream<Arguments> strictAvailabilityMappings() {
            return Stream.of(
                    Arguments.of(UserAvailability.AVAILABLE, null),
                    Arguments.of(UserAvailability.PENDING, TeamMemberErrorCode.USER_NOT_AVAILABLE),
                    Arguments.of(UserAvailability.UNAVAILABLE, TeamMemberErrorCode.USER_NOT_AVAILABLE),
                    Arguments.of(UserAvailability.NOT_FOUND, TeamMemberErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    class ValidateAvailableOrPending {

        @ParameterizedTest
        @MethodSource("invitationAvailabilityMappings")
        void allowsAvailableAndPendingUsers(UserAvailability availability, TeamMemberErrorCode expectedErrorCode) {
            when(userDirectory.check(ORGANIZATION_REFERENCE, USER_REFERENCE)).thenReturn(availability);

            assertAvailability(
                    expectedErrorCode,
                    () -> validator.validateAvailableOrPending(ORGANIZATION_REFERENCE, USER_REFERENCE));

            verify(userDirectory).check(ORGANIZATION_REFERENCE, USER_REFERENCE);
        }

        @Test
        void preservesTheCauseWhenTheUserDirectoryCannotBeReached() {
            UserDirectoryException directoryException =
                    new UserDirectoryException(new IllegalStateException("Identity unavailable"));
            when(userDirectory.check(ORGANIZATION_REFERENCE, USER_REFERENCE)).thenThrow(directoryException);

            TeamMemberException exception = assertThrows(
                    TeamMemberException.class,
                    () -> validator.validateAvailableOrPending(ORGANIZATION_REFERENCE, USER_REFERENCE));

            assertEquals(TeamMemberErrorCode.USER_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
            assertSame(directoryException, exception.getCause());
        }

        private static Stream<Arguments> invitationAvailabilityMappings() {
            return Stream.of(
                    Arguments.of(UserAvailability.AVAILABLE, null),
                    Arguments.of(UserAvailability.PENDING, null),
                    Arguments.of(UserAvailability.UNAVAILABLE, TeamMemberErrorCode.USER_NOT_AVAILABLE),
                    Arguments.of(UserAvailability.NOT_FOUND, TeamMemberErrorCode.USER_NOT_FOUND));
        }
    }

    private static void assertAvailability(
            TeamMemberErrorCode expectedErrorCode, org.junit.jupiter.api.function.Executable action) {
        if (expectedErrorCode == null) {
            assertDoesNotThrow(action);
            return;
        }

        TeamMemberException exception = assertThrows(TeamMemberException.class, action);

        assertEquals(expectedErrorCode, exception.getErrorCode());
    }
}
