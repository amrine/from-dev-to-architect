package io.teampulse.identity.domain.user.model;

import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final String USER_REFERENCE =
        "USR-2026-0908-00000ZA7B900";

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0908-00000ZA7B900";

    @Nested
    class CreationTests {

        @Test
        void createsDirectUserInCreatingStatus() {
            User user = createUser();

            assertEquals(USER_REFERENCE, user.getReference());
            assertEquals(ORGANIZATION_REFERENCE, user.getOrganizationReference());
            assertEquals("alice.smith@example.com", user.getEmail());
            assertEquals("Alice", user.getFirstName());
            assertEquals("Smith", user.getLastName());
            assertEquals(UserStatus.CREATING, user.getStatus());
        }

        @Test
        void invitesUserInInvitedStatus() {
            User user = inviteUser();

            assertEquals(UserStatus.INVITED, user.getStatus());
        }

        @Test
        void normalizesEmailToLowerCaseAndStripsSurroundingWhitespace() {
            User user = User.create(
                USER_REFERENCE,
                ORGANIZATION_REFERENCE,
                " Alice.Smith@Example.COM ",
                "Alice",
                "Smith"
            );

            assertEquals("alice.smith@example.com", user.getEmail());
        }

        @Test
        void stripsSurroundingWhitespaceFromNames() {
            User user = User.create(
                USER_REFERENCE,
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com",
                " Alice ",
                " Smith "
            );

            assertEquals("Alice", user.getFirstName());
            assertEquals("Smith", user.getLastName());
        }

        @Test
        void preservesInternalWhitespaceAndCaseInNames() {
            User user = User.create(
                USER_REFERENCE,
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com",
                "Mary  Jane",
                "de La Cruz"
            );

            assertEquals("Mary  Jane", user.getFirstName());
            assertEquals("de La Cruz", user.getLastName());
        }
    }

    @Nested
    class ValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "USR-2026-0908-00000ZA7B90",
            "USR-2026-0908-00000ZA7B9000",
            "usr-2026-0908-00000ZA7B900",
            "ORG-2026-0908-00000ZA7B900",
            " USR-2026-0908-00000ZA7B900"
        })
        void rejectsInvalidUserReference(String reference) {
            assertThrows(
                IllegalArgumentException.class,
                () -> User.create(
                    reference,
                    ORGANIZATION_REFERENCE,
                    "alice.smith@example.com",
                    "Alice",
                    "Smith"
                )
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "ORG-2026-0908-00000ZA7B90",
            "ORG-2026-0908-00000ZA7B9000",
            "org-2026-0908-00000ZA7B900",
            "USR-2026-0908-00000ZA7B900",
            "ORG-2026-0908-00000ZA7B900 "
        })
        void rejectsInvalidOrganizationReference(String organizationReference) {
            assertThrows(
                IllegalArgumentException.class,
                () -> User.create(
                    USER_REFERENCE,
                    organizationReference,
                    "alice.smith@example.com",
                    "Alice",
                    "Smith"
                )
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            " ",
            "alice",
            "@example.com",
            "alice@",
            "alice@@example.com",
            "alice @example.com"
        })
        void rejectsInvalidEmail(String email) {
            UserException exception = assertThrows(
                UserException.class,
                () -> User.create(
                    USER_REFERENCE,
                    ORGANIZATION_REFERENCE,
                    email,
                    "Alice",
                    "Smith"
                )
            );

            assertEquals(UserErrorCode.INVALID_EMAIL, exception.getErrorCode());
        }

        @Test
        void acceptsEmailAtMaximumLength() {
            String email = emailWithLength(254);

            User user = User.create(
                USER_REFERENCE,
                ORGANIZATION_REFERENCE,
                email,
                "Alice",
                "Smith"
            );

            assertEquals(email, user.getEmail());
        }

        @Test
        void rejectsEmailLongerThanMaximumLength() {
            UserException exception = assertThrows(
                UserException.class,
                () -> User.create(
                    USER_REFERENCE,
                    ORGANIZATION_REFERENCE,
                    emailWithLength(255),
                    "Alice",
                    "Smith"
                )
            );

            assertEquals(UserErrorCode.INVALID_EMAIL, exception.getErrorCode());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void rejectsInvalidFirstName(String firstName) {
            UserException exception = assertThrows(
                UserException.class,
                () -> User.create(
                    USER_REFERENCE,
                    ORGANIZATION_REFERENCE,
                    "alice.smith@example.com",
                    firstName,
                    "Smith"
                )
            );

            assertEquals(
                UserErrorCode.INVALID_FIRST_NAME,
                exception.getErrorCode()
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void rejectsInvalidLastName(String lastName) {
            UserException exception = assertThrows(
                UserException.class,
                () -> User.create(
                    USER_REFERENCE,
                    ORGANIZATION_REFERENCE,
                    "alice.smith@example.com",
                    "Alice",
                    lastName
                )
            );

            assertEquals(
                UserErrorCode.INVALID_LAST_NAME,
                exception.getErrorCode()
            );
        }

        @Test
        void acceptsNamesAtMaximumLength() {
            String name = "A".repeat(100);

            User user = User.create(
                USER_REFERENCE,
                ORGANIZATION_REFERENCE,
                "alice.smith@example.com",
                name,
                name
            );

            assertEquals(name, user.getFirstName());
            assertEquals(name, user.getLastName());
        }

        @Test
        void rejectsFirstNameLongerThanMaximumLength() {
            UserException exception = assertThrows(
                UserException.class,
                () -> User.create(
                    USER_REFERENCE,
                    ORGANIZATION_REFERENCE,
                    "alice.smith@example.com",
                    "A".repeat(101),
                    "Smith"
                )
            );

            assertEquals(
                UserErrorCode.INVALID_FIRST_NAME,
                exception.getErrorCode()
            );
        }

        @Test
        void rejectsLastNameLongerThanMaximumLength() {
            UserException exception = assertThrows(
                UserException.class,
                () -> User.create(
                    USER_REFERENCE,
                    ORGANIZATION_REFERENCE,
                    "alice.smith@example.com",
                    "Alice",
                    "S".repeat(101)
                )
            );

            assertEquals(
                UserErrorCode.INVALID_LAST_NAME,
                exception.getErrorCode()
            );
        }
    }

    @Nested
    class TransitionTests {

        @Test
        void exposesExplicitOperationsForCompleteLifecycle() {
            User user = inviteUser();

            user.startCreation();
            assertEquals(UserStatus.CREATING, user.getStatus());

            user.activate();
            assertEquals(UserStatus.ACTIVE, user.getStatus());

            user.suspend();
            assertEquals(UserStatus.SUSPENDED, user.getStatus());

            user.activate();
            assertEquals(UserStatus.ACTIVE, user.getStatus());

            user.deactivate();
            assertEquals(UserStatus.DEACTIVATED, user.getStatus());
        }

        @Test
        void leavesStateUntouchedWhenTransitionIsRejected() {
            User user = inviteUser();

            UserException exception = assertThrows(
                UserException.class,
                user::suspend
            );

            assertEquals(
                UserErrorCode.INVALID_STATUS_TRANSITION,
                exception.getErrorCode()
            );
            assertEquals(UserStatus.INVITED, user.getStatus());
        }

        @Test
        void cannotLeaveDeactivatedStatus() {
            User user = createUser();
            user.deactivate();

            UserException exception = assertThrows(
                UserException.class,
                user::activate
            );

            assertEquals(
                UserErrorCode.INVALID_STATUS_TRANSITION,
                exception.getErrorCode()
            );
            assertEquals(UserStatus.DEACTIVATED, user.getStatus());
        }
    }

    @Nested
    class RestorationTests {

        @Test
        void restoresPersistedBusinessState() {
            User user = restoredActiveUser();

            assertEquals(USER_REFERENCE, user.getReference());
            assertEquals(ORGANIZATION_REFERENCE, user.getOrganizationReference());
            assertEquals("alice.smith@example.com", user.getEmail());
            assertEquals(UserStatus.ACTIVE, user.getStatus());
        }
    }

    private static User createUser() {
        return User.create(
            USER_REFERENCE,
            ORGANIZATION_REFERENCE,
            "alice.smith@example.com",
            "Alice",
            "Smith"
        );
    }

    private static User inviteUser() {
        return User.invite(
            USER_REFERENCE,
            ORGANIZATION_REFERENCE,
            "alice.smith@example.com",
            "Alice",
            "Smith"
        );
    }

    private static User restoredActiveUser() {
        return User.restore(
            USER_REFERENCE,
            ORGANIZATION_REFERENCE,
            "alice.smith@example.com",
            "Alice",
            "Smith",
            UserStatus.ACTIVE
        );
    }

    private static String emailWithLength(int length) {
        String suffix = "@example.com";
        return "a".repeat(length - suffix.length()) + suffix;
    }
}
