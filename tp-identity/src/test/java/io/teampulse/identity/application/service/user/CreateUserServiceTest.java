package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.application.port.in.user.CreateUserCommand;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-3008-00000ZA7B900";
    private static final String USER_REFERENCE =
        "USR-2026-3008-00000ZA7B900";

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReferenceFactory referenceFactory;
    @InjectMocks
    private CreateUserService service;

    @Nested
    class CreationTests {

        @Test
        void generatesAUserReferenceWithUsrPrefix() {
            prepareSuccessfulCreation();

            service.create(validTenantContext(), validCommand());

            InOrder orderedInteractions = inOrder(referenceFactory, userRepository);
            orderedInteractions.verify(referenceFactory).generate("USR");
            orderedInteractions.verify(userRepository).existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            );
            orderedInteractions.verify(userRepository).create(any(User.class));
        }

        @Test
        void passesTenantReferenceAsOrganizationReference() {
            prepareSuccessfulCreation();
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            service.create(validTenantContext(), validCommand());

            verify(userRepository).existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            );
            verify(userRepository).create(userCaptor.capture());
            assertEquals(
                ORGANIZATION_REFERENCE,
                userCaptor.getValue().getOrganizationReference()
            );
        }

        @Test
        void checksUniquenessWithCanonicalEmail() {
            prepareSuccessfulCreation();

            service.create(
                validTenantContext(),
                new CreateUserCommand(
                    " Alice.Smith@Example.COM ",
                    "Alice",
                    "Smith"
                )
            );

            verify(userRepository).existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            );
        }

        @Test
        void persistsAndReturnsCreatedUser() {
            when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);
            when(userRepository.existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            )).thenReturn(false);
            User persistedUser = User.create(
                USER_REFERENCE,
                ORGANIZATION_REFERENCE,
                "persisted@example.com",
                "Persisted",
                "User"
            );
            when(userRepository.create(any(User.class))).thenReturn(persistedUser);

            User result = service.create(validTenantContext(), validCommand());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).create(userCaptor.capture());
            assertEquals(USER_REFERENCE, userCaptor.getValue().getReference());
            assertEquals("alice.smith@example.com", userCaptor.getValue().getEmail());
            assertSame(persistedUser, result);
        }
    }

    @Nested
    class RejectionTests {

        @Test
        void returnsEmailAlreadyUsedAndDoesNotPersistDuplicate() {
            when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);
            when(userRepository.existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            )).thenReturn(true);

            UserException exception = assertThrows(
                UserException.class,
                () -> service.create(validTenantContext(), validCommand())
            );

            assertEquals(UserErrorCode.EMAIL_ALREADY_USED, exception.getErrorCode());
            verify(userRepository, never()).create(any(User.class));
        }

        @ParameterizedTest
        @MethodSource("invalidCommands")
        void propagatesUserCreationValidationErrors(
            CreateUserCommand command,
            UserErrorCode expectedErrorCode
        ) {
            when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);

            UserException exception = assertThrows(
                UserException.class,
                () -> service.create(validTenantContext(), command)
            );

            assertEquals(expectedErrorCode, exception.getErrorCode());
            verifyNoInteractions(userRepository);
        }

        static Stream<Arguments> invalidCommands() {
            return Stream.of(
                Arguments.of(
                    new CreateUserCommand("invalid-email", "Alice", "Smith"),
                    UserErrorCode.INVALID_EMAIL
                ),
                Arguments.of(
                    new CreateUserCommand(
                        "alice.smith@example.com",
                        "A".repeat(101),
                        "Smith"
                    ),
                    UserErrorCode.INVALID_FIRST_NAME
                ),
                Arguments.of(
                    new CreateUserCommand(
                        "alice.smith@example.com",
                        "Alice",
                        "S".repeat(101)
                    ),
                    UserErrorCode.INVALID_LAST_NAME
                )
            );
        }

        @Test
        void doesNotPersistWhenReferenceGenerationFails() {
            IllegalStateException generationFailure =
                new IllegalStateException("Reference generation failed");
            when(referenceFactory.generate("USR")).thenThrow(generationFailure);

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.create(validTenantContext(), validCommand())
            );

            assertSame(generationFailure, exception);
            verifyNoInteractions(userRepository);
        }
    }

    private void prepareSuccessfulCreation() {
        when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);
        when(userRepository.existsByEmail(
            ORGANIZATION_REFERENCE,
            "alice.smith@example.com"
        )).thenReturn(false);
        when(userRepository.create(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static TenantContext validTenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private static CreateUserCommand validCommand() {
        return new CreateUserCommand(
            "alice.smith@example.com",
            "Alice",
            "Smith"
        );
    }
}
