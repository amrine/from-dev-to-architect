package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationResponsibleUsersValidatorTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1309-00000ZA7B900";
    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1309-00000ZA7B901";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1309-00000ZA7B902";

    @Mock(mockMaker = MockMakers.PROXY)
    private UserDirectory userDirectory;
    @InjectMocks
    private OrganizationResponsibleUsersValidator validator;

    @Nested
    class AssignableResponsibleTests {

        @ParameterizedTest
        @MethodSource("administratorAssignableMappings")
        void mapsEveryAdministratorAvailability(
            UserAvailability availability,
            OrganizationErrorCode expectedErrorCode
        ) {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(availability);

            if (expectedErrorCode == null) {
                assertEquals(
                    availability,
                    validator.validateAssignableAdministrator(
                        ORGANIZATION_REFERENCE,
                        ADMINISTRATOR_REFERENCE
                    )
                );
            } else {
                assertErrorCode(
                    expectedErrorCode,
                    () -> validator.validateAssignableAdministrator(
                        ORGANIZATION_REFERENCE,
                        ADMINISTRATOR_REFERENCE
                    )
                );
            }

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            );
        }

        @ParameterizedTest
        @MethodSource("managerAssignableMappings")
        void mapsEveryManagerAvailability(
            UserAvailability availability,
            OrganizationErrorCode expectedErrorCode
        ) {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(availability);

            if (expectedErrorCode == null) {
                assertEquals(
                    availability,
                    validator.validateAssignableManager(
                        ORGANIZATION_REFERENCE,
                        MANAGER_REFERENCE
                    )
                );
            } else {
                assertErrorCode(
                    expectedErrorCode,
                    () -> validator.validateAssignableManager(
                        ORGANIZATION_REFERENCE,
                        MANAGER_REFERENCE
                    )
                );
            }

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            );
        }

        static Stream<Arguments> administratorAssignableMappings() {
            return Stream.of(
                Arguments.of(UserAvailability.AVAILABLE, null),
                Arguments.of(UserAvailability.PENDING, null),
                Arguments.of(
                    UserAvailability.UNAVAILABLE,
                    OrganizationErrorCode.ADMINISTRATOR_NOT_AVAILABLE
                ),
                Arguments.of(
                    UserAvailability.NOT_FOUND,
                    OrganizationErrorCode.ADMINISTRATOR_NOT_FOUND
                )
            );
        }

        static Stream<Arguments> managerAssignableMappings() {
            return Stream.of(
                Arguments.of(UserAvailability.AVAILABLE, null),
                Arguments.of(UserAvailability.PENDING, null),
                Arguments.of(
                    UserAvailability.UNAVAILABLE,
                    OrganizationErrorCode.MANAGER_NOT_AVAILABLE
                ),
                Arguments.of(
                    UserAvailability.NOT_FOUND,
                    OrganizationErrorCode.MANAGER_NOT_FOUND
                )
            );
        }
    }

    @Nested
    class OperationalResponsibleTests {

        @ParameterizedTest
        @MethodSource("administratorOperationalMappings")
        void mapsEveryAdministratorAvailability(
            UserAvailability availability,
            OrganizationErrorCode expectedErrorCode
        ) {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(availability);

            if (expectedErrorCode == null) {
                validator.validateOperationalAdministrator(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE
                );
            } else {
                assertErrorCode(
                    expectedErrorCode,
                    () -> validator.validateOperationalAdministrator(
                        ORGANIZATION_REFERENCE,
                        ADMINISTRATOR_REFERENCE
                    )
                );
            }

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            );
        }

        @ParameterizedTest
        @MethodSource("managerOperationalMappings")
        void mapsEveryManagerAvailability(
            UserAvailability availability,
            OrganizationErrorCode expectedErrorCode
        ) {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(availability);

            if (expectedErrorCode == null) {
                validator.validateOperationalManager(
                    ORGANIZATION_REFERENCE,
                    MANAGER_REFERENCE
                );
            } else {
                assertErrorCode(
                    expectedErrorCode,
                    () -> validator.validateOperationalManager(
                        ORGANIZATION_REFERENCE,
                        MANAGER_REFERENCE
                    )
                );
            }

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            );
        }

        @Test
        void checksASharedResponsibleReferenceOnlyOnce() {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);

            validator.validateOperationalResponsibleUsers(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE,
                ADMINISTRATOR_REFERENCE
            );

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            );
        }

        @Test
        void givesAdministratorErrorPriorityForASharedReference() {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.PENDING);

            assertErrorCode(
                OrganizationErrorCode.ADMINISTRATOR_NOT_AVAILABLE,
                () -> validator.validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    ADMINISTRATOR_REFERENCE
                )
            );

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            );
        }

        @Test
        void mapsTheManagerErrorWhenDistinctResponsibleUsersAreChecked() {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(UserAvailability.NOT_FOUND);

            assertErrorCode(
                OrganizationErrorCode.MANAGER_NOT_FOUND,
                () -> validator.validateOperationalResponsibleUsers(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                )
            );

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            );
            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            );
        }

        static Stream<Arguments> administratorOperationalMappings() {
            return Stream.of(
                Arguments.of(UserAvailability.AVAILABLE, null),
                Arguments.of(
                    UserAvailability.PENDING,
                    OrganizationErrorCode.ADMINISTRATOR_NOT_AVAILABLE
                ),
                Arguments.of(
                    UserAvailability.UNAVAILABLE,
                    OrganizationErrorCode.ADMINISTRATOR_NOT_AVAILABLE
                ),
                Arguments.of(
                    UserAvailability.NOT_FOUND,
                    OrganizationErrorCode.ADMINISTRATOR_NOT_FOUND
                )
            );
        }

        static Stream<Arguments> managerOperationalMappings() {
            return Stream.of(
                Arguments.of(UserAvailability.AVAILABLE, null),
                Arguments.of(
                    UserAvailability.PENDING,
                    OrganizationErrorCode.MANAGER_NOT_AVAILABLE
                ),
                Arguments.of(
                    UserAvailability.UNAVAILABLE,
                    OrganizationErrorCode.MANAGER_NOT_AVAILABLE
                ),
                Arguments.of(
                    UserAvailability.NOT_FOUND,
                    OrganizationErrorCode.MANAGER_NOT_FOUND
                )
            );
        }
    }

    @Nested
    class AvailabilityProbeTests {

        @Test
        void returnsTrueWhenBothResponsibleUsersAreAvailable() {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);

            assertTrue(
                validator.areResponsibleUsersAvailable(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                )
            );
        }

        @ParameterizedTest
        @MethodSource("nonAvailableStates")
        void returnsFalseWhenAResponsibleUserIsNotAvailable(
            UserAvailability availability
        ) {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                MANAGER_REFERENCE
            )).thenReturn(availability);

            assertFalse(
                validator.areResponsibleUsersAvailable(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    MANAGER_REFERENCE
                )
            );
        }

        @Test
        void checksASharedResponsibleReferenceOnlyOnce() {
            when(userDirectory.check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            )).thenReturn(UserAvailability.AVAILABLE);

            assertTrue(
                validator.areResponsibleUsersAvailable(
                    ORGANIZATION_REFERENCE,
                    ADMINISTRATOR_REFERENCE,
                    ADMINISTRATOR_REFERENCE
                )
            );

            verify(userDirectory).check(
                ORGANIZATION_REFERENCE,
                ADMINISTRATOR_REFERENCE
            );
        }

        static Stream<Arguments> nonAvailableStates() {
            return Stream.of(
                Arguments.of(UserAvailability.PENDING),
                Arguments.of(UserAvailability.UNAVAILABLE),
                Arguments.of(UserAvailability.NOT_FOUND)
            );
        }
    }

    @Test
    void mapsUserDirectoryFailureAndPreservesItsCause() {
        IllegalStateException directoryCause =
            new IllegalStateException("Identity repository unavailable");
        UserDirectoryException directoryFailure =
            new UserDirectoryException(directoryCause);
        when(userDirectory.check(
            ORGANIZATION_REFERENCE,
            ADMINISTRATOR_REFERENCE
        )).thenThrow(directoryFailure);

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> validator.validateAssignableAdministrator(
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

    private static void assertErrorCode(
        OrganizationErrorCode expectedErrorCode,
        Runnable validation
    ) {
        OrganizationException exception = assertThrows(
            OrganizationException.class,
            validation::run
        );

        assertEquals(expectedErrorCode, exception.getErrorCode());
    }
}
