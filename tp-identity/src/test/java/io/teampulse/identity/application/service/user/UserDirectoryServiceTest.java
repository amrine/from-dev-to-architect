package io.teampulse.identity.application.service.user;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0209-00000ZA7B920";
    private static final String USER_REFERENCE =
        "USR-2026-0209-00000ZA7B920";

    @Mock(mockMaker = MockMakers.PROXY)
    private UserRepository userRepository;

    @InjectMocks
    private UserDirectoryService service;


    @ParameterizedTest
    @MethodSource("availabilityMappings")
    void mapsEveryUserStatus(
        UserStatus status,
        UserAvailability expectedAvailability
    ) {
        // GIVEN
        when(userRepository.findByReference(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        )).thenReturn(Optional.of(userWithStatus(status)));

        // WHEN
        UserAvailability availability = service.check(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        );

        // THEN
        assertEquals(expectedAvailability, availability);
        verify(userRepository).findByReference(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        );
    }

    static Stream<Arguments> availabilityMappings() {
        return Stream.of(
            Arguments.of(UserStatus.ACTIVE, UserAvailability.AVAILABLE),
            Arguments.of(UserStatus.INVITED, UserAvailability.PENDING),
            Arguments.of(UserStatus.CREATING, UserAvailability.PENDING),
            Arguments.of(UserStatus.SUSPENDED, UserAvailability.UNAVAILABLE),
            Arguments.of(UserStatus.DEACTIVATED, UserAvailability.UNAVAILABLE)
        );
    }

    @Test
    void returnsNotFoundAndPassesBothReferencesWhenUserIsAbsent() {
        // GIVEN
        when(userRepository.findByReference(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        )).thenReturn(Optional.empty());

        // WHEN
        UserAvailability availability = service.check(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        );

        // THEN
        assertEquals(UserAvailability.NOT_FOUND, availability);
        verify(userRepository).findByReference(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        );
    }

    @Test
    void wrapsRepositoryRuntimeExceptionAndPreservesCause() {
        // GIVEN
        RuntimeException repositoryFailure =
            new IllegalStateException("Repository unavailable");
        when(userRepository.findByReference(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        )).thenThrow(repositoryFailure);

        // WHEN
        UserDirectoryException exception = assertThrows(
            UserDirectoryException.class,
            () -> service.check(ORGANIZATION_REFERENCE, USER_REFERENCE)
        );

        // THEN
        assertEquals("Unable to check user availability", exception.getMessage());
        assertEquals(repositoryFailure, exception.getCause());
    }

    @Test
    void wrapsInternalUserException() {
        UserException internalFailure = new UserException(
            UserErrorCode.NOT_FOUND,
            "Internal identity failure"
        );
        when(userRepository.findByReference(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        )).thenThrow(internalFailure);

        UserDirectoryException exception = assertThrows(
            UserDirectoryException.class,
            () -> service.check(ORGANIZATION_REFERENCE, USER_REFERENCE)
        );

        assertEquals(internalFailure, exception.getCause());
    }

    @Test
    void doesNotWrapErrors() {
        AssertionError error = new AssertionError("Fatal failure");
        when(userRepository.findByReference(
            ORGANIZATION_REFERENCE,
            USER_REFERENCE
        )).thenThrow(error);

        AssertionError thrown = assertThrows(
            AssertionError.class,
            () -> service.check(ORGANIZATION_REFERENCE, USER_REFERENCE)
        );

        assertEquals(error, thrown);
    }

    private static User userWithStatus(UserStatus status) {
        return User.restore(
            USER_REFERENCE,
            ORGANIZATION_REFERENCE,
            "alice@example.com",
            "Alice",
            "User",
            status
        );
    }
}
