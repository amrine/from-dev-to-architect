package io.teampulse.team.application.service.team;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
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

    @ParameterizedTest
    @MethodSource("administratorAvailabilityMappings")
    void mapsAdministratorAvailability(UserAvailability availability, TeamErrorCode expectedErrorCode) {
        when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                .thenReturn(availability);

        if (expectedErrorCode == null) {
            validator.validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, ADMINISTRATOR_REFERENCE);
        } else {
            assertErrorCode(
                    expectedErrorCode,
                    () -> validator.validateOperationalResponsibleUsers(
                            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, ADMINISTRATOR_REFERENCE));
        }

        verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE);
    }

    @ParameterizedTest
    @MethodSource("managerAvailabilityMappings")
    void mapsManagerAvailability(UserAvailability availability, TeamErrorCode expectedErrorCode) {
        when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                .thenReturn(UserAvailability.AVAILABLE);
        when(userDirectory.check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE)).thenReturn(availability);

        if (expectedErrorCode == null) {
            validator.validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);
        } else {
            assertErrorCode(
                    expectedErrorCode,
                    () -> validator.validateOperationalResponsibleUsers(
                            ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE));
        }

        verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);
    }

    @Test
    void preservesTheCauseWhenTheUserDirectoryCannotBeReached() {
        IllegalStateException cause = new IllegalStateException("Identity unavailable");
        UserDirectoryException directoryException = new UserDirectoryException(cause);
        when(userDirectory.check(ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE))
                .thenThrow(directoryException);

        TeamException exception = assertThrows(
                TeamException.class,
                () -> validator.validateOperationalResponsibleUsers(
                        ORGANIZATION_REFERENCE, ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE));

        assertEquals(TeamErrorCode.USER_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
        assertSame(directoryException, exception.getCause());
    }

    private static Stream<Arguments> administratorAvailabilityMappings() {
        return Stream.of(
                Arguments.of(UserAvailability.AVAILABLE, null),
                Arguments.of(UserAvailability.PENDING, TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE),
                Arguments.of(UserAvailability.UNAVAILABLE, TeamErrorCode.ADMINISTRATOR_NOT_AVAILABLE),
                Arguments.of(UserAvailability.NOT_FOUND, TeamErrorCode.ADMINISTRATOR_NOT_FOUND));
    }

    private static Stream<Arguments> managerAvailabilityMappings() {
        return Stream.of(
                Arguments.of(UserAvailability.AVAILABLE, null),
                Arguments.of(UserAvailability.PENDING, TeamErrorCode.MANAGER_NOT_AVAILABLE),
                Arguments.of(UserAvailability.UNAVAILABLE, TeamErrorCode.MANAGER_NOT_AVAILABLE),
                Arguments.of(UserAvailability.NOT_FOUND, TeamErrorCode.MANAGER_NOT_FOUND));
    }

    private static void assertErrorCode(
            TeamErrorCode expectedErrorCode, org.junit.jupiter.api.function.Executable executable) {
        TeamException exception = assertThrows(TeamException.class, executable);

        assertEquals(expectedErrorCode, exception.getErrorCode());
    }
}
