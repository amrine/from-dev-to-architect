package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.api.lifecycle.UserLifecycleErrorCode;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.out.user.IdentityEventPublisher;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.events.UserInvited;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLifecycleServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-3008-00000ZA7B900";
    private static final String USER_REFERENCE =
        "USR-2026-3008-00000ZA7B900";

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReferenceFactory referenceFactory;
    @Mock
    private IdentityEventPublisher invitationEventPublisher;
    @InjectMocks
    private UserLifecycleService service;

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
                new UserProvisioningCommand(
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
        void persistsAndReturnsPublicLifecycleResult() {
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

            UserLifecycleResult result = service.create(validTenantContext(), validCommand());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).create(userCaptor.capture());
            assertEquals(USER_REFERENCE, userCaptor.getValue().getReference());
            assertEquals("alice.smith@example.com", userCaptor.getValue().getEmail());
            assertEquals(USER_REFERENCE, result.userReference());
            assertEquals("persisted@example.com", result.email());
            assertEquals("Persisted", result.firstName());
            assertEquals("User", result.lastName());
            assertEquals("CREATING", result.status());
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

            UserLifecycleException exception = assertThrows(
                UserLifecycleException.class,
                () -> service.create(validTenantContext(), validCommand())
            );

            assertEquals(UserLifecycleErrorCode.EMAIL_ALREADY_USED, exception.getErrorCode());
            verify(userRepository, never()).create(any(User.class));
        }

        @ParameterizedTest
        @MethodSource("invalidCommands")
        void propagatesUserCreationValidationErrors(
            UserProvisioningCommand command,
            UserLifecycleErrorCode expectedErrorCode
        ) {
            when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);

            UserLifecycleException exception = assertThrows(
                UserLifecycleException.class,
                () -> service.create(validTenantContext(), command)
            );

            assertEquals(expectedErrorCode, exception.getErrorCode());
            verifyNoInteractions(userRepository);
        }

        static Stream<Arguments> invalidCommands() {
            return Stream.of(
                Arguments.of(
                    new UserProvisioningCommand("invalid-email", "Alice", "Smith"),
                    UserLifecycleErrorCode.INVALID_USER_DATA
                ),
                Arguments.of(
                    new UserProvisioningCommand(
                        "alice.smith@example.com",
                        "A".repeat(101),
                        "Smith"
                    ),
                    UserLifecycleErrorCode.INVALID_USER_DATA
                ),
                Arguments.of(
                    new UserProvisioningCommand(
                        "alice.smith@example.com",
                        "Alice",
                        "S".repeat(101)
                    ),
                    UserLifecycleErrorCode.INVALID_USER_DATA
                )
            );
        }

        @Test
        void doesNotPersistWhenReferenceGenerationFails() {
            IllegalStateException generationFailure =
                new IllegalStateException("Reference generation failed");
            when(referenceFactory.generate("USR")).thenThrow(generationFailure);

            UserLifecycleException exception = assertThrows(
                UserLifecycleException.class,
                () -> service.create(validTenantContext(), validCommand())
            );

            assertEquals(UserLifecycleErrorCode.OPERATION_FAILED, exception.getErrorCode());
            assertSame(generationFailure, exception.getCause());
            verifyNoInteractions(userRepository);
        }

        @Test
        void mapsTechnicalRepositoryFailuresAndPreservesTheirCause() {
            IllegalStateException persistenceFailure =
                new IllegalStateException("Database unavailable");
            when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);
            when(userRepository.existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            )).thenThrow(persistenceFailure);

            UserLifecycleException exception = assertThrows(
                UserLifecycleException.class,
                () -> service.create(validTenantContext(), validCommand())
            );

            assertEquals(UserLifecycleErrorCode.OPERATION_FAILED, exception.getErrorCode());
            assertSame(persistenceFailure, exception.getCause());
            verify(userRepository, never()).create(any(User.class));
        }
    }

    @Nested
    class InvitationTests {

        @Test
        void persistsAnInvitedUserAndPublishesOnlyReferencesAfterPersistence() {
            when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);
            when(userRepository.existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            )).thenReturn(false);
            when(userRepository.create(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            UserLifecycleResult invitedUser = service.invite(validTenantContext(), validCommand());

            assertEquals("INVITED", invitedUser.status());
            InOrder orderedInteractions = inOrder(
                referenceFactory,
                userRepository,
                invitationEventPublisher
            );
            orderedInteractions.verify(referenceFactory).generate("USR");
            orderedInteractions.verify(userRepository).existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            );
            orderedInteractions.verify(userRepository).create(any(User.class));
            orderedInteractions.verify(invitationEventPublisher).publish(
                new UserInvited(ORGANIZATION_REFERENCE, USER_REFERENCE)
            );
        }

        @Test
        void doesNotPublishAnInvitationWhenTheEmailIsAlreadyUsed() {
            when(referenceFactory.generate("USR")).thenReturn(USER_REFERENCE);
            when(userRepository.existsByEmail(
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com"
            )).thenReturn(true);

            assertThrows(
                UserLifecycleException.class,
                () -> service.invite(validTenantContext(), validCommand())
            );

            verify(userRepository, never()).create(any(User.class));
            verifyNoInteractions(invitationEventPublisher);
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

    private static UserProvisioningCommand validCommand() {
        return new UserProvisioningCommand(
            "alice.smith@example.com",
            "Alice",
            "Smith"
        );
    }
}
