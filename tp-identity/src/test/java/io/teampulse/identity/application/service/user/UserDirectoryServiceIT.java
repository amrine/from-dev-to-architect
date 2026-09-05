package io.teampulse.identity.application.service.user;

import io.teampulse.identity.AbstractIntegrationTest;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.identity.infrastructure.persistence.repository.JpaUserRepository;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDirectoryServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_A =
        "ORG-2026-0209-00000ZA7B930";
    private static final String ORGANIZATION_B =
        "ORG-2026-0209-00000ZA7B931";
    private static final String USER_REFERENCE =
        "USR-2026-0209-00000ZA7B930";

    @Inject
    private UserDirectory userDirectory;

    @Inject
    private JpaUserRepository jpaUserRepository;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaUserRepository::deleteAllInBatch);
    }

    @Test
    void findsUserFromRequestedOrganizationThroughPublicContract() {
        inTransactionTemplate(() -> jpaUserRepository.save(userEntity(ORGANIZATION_A, UserStatus.ACTIVE)));

        UserAvailability availability = userDirectory.check(
            ORGANIZATION_A,
            USER_REFERENCE
        );

        assertEquals(UserAvailability.AVAILABLE, availability);
    }

    @Test
    void returnsNotFoundForMissingUser() {
        UserAvailability availability = userDirectory.check(
            ORGANIZATION_A,
            USER_REFERENCE
        );

        assertEquals(UserAvailability.NOT_FOUND, availability);
    }

    @Test
    void doesNotDiscloseUserFromAnotherOrganization() {
        inTransactionTemplate(() -> jpaUserRepository.save(userEntity(ORGANIZATION_B, UserStatus.ACTIVE)));

        UserAvailability availability = userDirectory.check(
            ORGANIZATION_A,
            USER_REFERENCE
        );

        assertEquals(UserAvailability.NOT_FOUND, availability);
    }

    @Test
    void checksUserInsideAnActiveReadOnlyTransaction() {
        userDirectory.check(ORGANIZATION_A, USER_REFERENCE);

        TransactionManagerProbe.TransactionObservation observation =
            transactionProbe.observation();
        assertTrue(
            observation.name().endsWith("UserDirectoryService.check")
        );
        assertTrue(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    @ParameterizedTest
    @MethodSource("invalidReferences")
    void rejectsInvalidReferencesAsContractViolations(
        String organizationReference,
        String userReference
    ) {
        RuntimeException exception = assertThrows(
            ConstraintViolationException.class,
            () -> userDirectory.check(organizationReference, userReference)
        );

        assertFalse(exception instanceof UserDirectoryException);
    }

    static Stream<Arguments> invalidReferences() {
        return Stream.of(
            Arguments.of(null, USER_REFERENCE),
            Arguments.of("", USER_REFERENCE),
            Arguments.of("   ", USER_REFERENCE),
            Arguments.of(ORGANIZATION_A, null),
            Arguments.of(ORGANIZATION_A, ""),
            Arguments.of(ORGANIZATION_A, "   ")
        );
    }

    private static UserEntity userEntity(
        String organizationReference,
        UserStatus status
    ) {
        return new UserEntity()
            .setReference(USER_REFERENCE)
            .setOrganizationReference(organizationReference)
            .setEmail("alice@example.com")
            .setFirstName("Alice")
            .setLastName("User")
            .setStatus(status);
    }
}
