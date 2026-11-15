package io.teampulse.team.application.service.team;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamResponsibleUsersValidatorTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B900";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-1309-00000ZA7B901";
    private static final String MANAGER_REFERENCE = "USR-2026-1309-00000ZA7B902";

    @Mock(mockMaker = MockMakers.PROXY)
    private UserDirectory userDirectory;

    @InjectMocks
    private TeamResponsibleUsersValidator validator;

    @Nested
    class ValidateAdministrator {

        @ParameterizedTest
        @MethodSource("availabilityMappings")
        void mapsAdministratorAvailability(UserAvailability availability, TeamErrorCode expectedErrorCode) {
            when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                    .thenReturn(availability);

            assertAvailability(expectedErrorCode, ADMINISTRATOR_REFERENCE, ADMINISTRATOR_REFERENCE);

            verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE);
        }

        @Test
        void stopsBeforeCheckingTheManagerWhenTheAdministratorIsRejected() {
            when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                    .thenReturn(UserAvailability.UNAVAILABLE);

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> validator.validateOperationalResponsibleUsers(
                            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE));

            assertEquals(TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE, exception.getErrorCode());
            verify(userDirectory, never()).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
        }

        @Test
        void preservesTheCauseWhenTheAdministratorDirectoryCheckFails() {
            UserDirectoryException directoryException = directoryFailure();
            when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                    .thenThrow(directoryException);

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> validator.validateOperationalResponsibleUsers(
                            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE));

            assertDirectoryFailure(exception, directoryException);
            verify(userDirectory, never()).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
        }

        private static Stream<Arguments> availabilityMappings() {
            return Stream.of(
                    Arguments.of(UserAvailability.AVAILABLE, null),
                    Arguments.of(UserAvailability.PENDING, TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE),
                    Arguments.of(UserAvailability.UNAVAILABLE, TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE),
                    Arguments.of(UserAvailability.NOT_FOUND, TeamErrorCode.ADMINISTRATOR_NOT_FOUND));
        }
    }

    @Nested
    class ValidateManager {

        @ParameterizedTest
        @MethodSource("availabilityMappings")
        void mapsManagerAvailability(UserAvailability availability, TeamErrorCode expectedErrorCode) {
            when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                    .thenReturn(UserAvailability.AVAILABLE);
            when(userDirectory.check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE)).thenReturn(availability);

            assertAvailability(expectedErrorCode, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);

            verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
        }

        @Test
        void preservesTheCauseWhenTheManagerDirectoryCheckFails() {
            UserDirectoryException directoryException = directoryFailure();
            when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                    .thenReturn(UserAvailability.AVAILABLE);
            when(userDirectory.check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE)).thenThrow(directoryException);

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> validator.validateOperationalResponsibleUsers(
                            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE));

            assertDirectoryFailure(exception, directoryException);
        }

        private static Stream<Arguments> availabilityMappings() {
            return Stream.of(
                    Arguments.of(UserAvailability.AVAILABLE, null),
                    Arguments.of(UserAvailability.PENDING, TeamErrorCode.MANAGER_NOT_AVAILABLE),
                    Arguments.of(UserAvailability.UNAVAILABLE, TeamErrorCode.MANAGER_NOT_AVAILABLE),
                    Arguments.of(UserAvailability.NOT_FOUND, TeamErrorCode.MANAGER_NOT_FOUND));
        }
    }

    private void assertAvailability(
            TeamErrorCode expectedErrorCode, String administratorReference, String managerReference) {
        if (expectedErrorCode == null) {
            validator.validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE, administratorReference, managerReference);
            return;
        }

        TeamException exception = assertThrows(
                TeamException.class,
                () -> validator.validateOperationalResponsibleUsers(
                        ORGANIZATION_REFERENCE, administratorReference, managerReference));

        assertEquals(expectedErrorCode, exception.getErrorCode());
    }

    private static UserDirectoryException directoryFailure() {
        return new UserDirectoryException(new IllegalStateException("Identity unavailable"));
    }

    private static void assertDirectoryFailure(TeamException exception, UserDirectoryException directoryException) {
        assertEquals(TeamErrorCode.USER_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
        assertSame(directoryException, exception.getCause());
    }
}
