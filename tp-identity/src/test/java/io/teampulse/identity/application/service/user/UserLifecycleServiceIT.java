package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.identity.AbstractIT;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserLifecycleErrorCode;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.identity.infrastructure.persistence.repository.JpaUserRepository;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLifecycleServiceIT extends AbstractIT {

    private static final String ORGANIZATION_A =
        "ORG-2026-3108-00000ZA7B900";
    private static final String ORGANIZATION_B =
        "ORG-2026-3108-00000ZA7B901";

    @Inject
    private UserLifecycle userLifecycle;

    @Inject
    private JpaUserRepository jpaUserRepository;

    @Inject
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaUserRepository::deleteAllInBatch);
    }

    @Test
    void rejectsNullTenantContextBeforePersisting() {
        assertThrows(
            ConstraintViolationException.class,
            () -> userLifecycle.create(null, validCommand())
        );

        assertEquals(0L, jpaUserRepository.count());
    }

    @Test
    void rejectsNullCommandBeforePersisting() {
        assertThrows(
            ConstraintViolationException.class,
            () -> userLifecycle.create(tenant(ORGANIZATION_A), null)
        );

        assertEquals(0L, jpaUserRepository.count());
    }

    @ParameterizedTest
    @MethodSource("commandsWithBlankRequiredField")
    void rejectsBlankCommandFieldsBeforePersisting(UserProvisioningCommand command) {
        assertThrows(
            ConstraintViolationException.class,
            () -> userLifecycle.create(tenant(ORGANIZATION_A), command)
        );

        assertEquals(0L, jpaUserRepository.count());
    }

    @Test
    void createsAndReturnsThePersistedUserLifecycleResult() {
        UserLifecycleResult createdUser = userLifecycle.create(
            tenant(ORGANIZATION_A),
            new UserProvisioningCommand(
                " Alice.Smith@Example.COM ",
                " Alice ",
                " Smith "
            )
        );

        assertTrue(ReferenceFormat.matches(createdUser.userReference(), "USR"));
        assertEquals("alice.smith@example.com", createdUser.email());
        assertEquals("Alice", createdUser.firstName());
        assertEquals("Smith", createdUser.lastName());
        assertEquals("CREATING", createdUser.status());

        UserEntity storedUser = findStoredUser(
            ORGANIZATION_A,
            createdUser.userReference()
        );
        assertEquals(createdUser.userReference(), storedUser.getReference());
        assertEquals(ORGANIZATION_A, storedUser.getOrganizationReference());
        assertEquals("alice.smith@example.com", storedUser.getEmail());
        assertEquals("Alice", storedUser.getFirstName());
        assertEquals("Smith", storedUser.getLastName());
        assertEquals(UserStatus.CREATING, storedUser.getStatus());
    }

    @Test
    void createsUserInsideAnActiveReadWriteTransaction() {
        userLifecycle.create(
            tenant(ORGANIZATION_A),
            validCommand()
        );

        TransactionManagerProbe.TransactionObservation observation =
            transactionProbe.observation();
        assertTrue(
            observation.name().endsWith("UserLifecycleService.create")
        );
        assertFalse(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    @Test
    void rejectsCanonicalEmailAlreadyUsedInTheSameOrganization() {
        userLifecycle.create(
            tenant(ORGANIZATION_A),
            validCommand()
        );

        UserLifecycleException exception = assertThrows(
            UserLifecycleException.class,
            () -> userLifecycle.create(
                tenant(ORGANIZATION_A),
                new UserProvisioningCommand(
                    " ALICE.SMITH@EXAMPLE.COM ",
                    "Another",
                    "User"
                )
            )
        );

        assertEquals(UserLifecycleErrorCode.EMAIL_ALREADY_USED, exception.getErrorCode());
        assertEquals(1L, jpaUserRepository.count());
    }

    @Test
    void invitesAndPersistsAUserInInvitedStatus() {
        UserLifecycleResult invitedUser = userLifecycle.invite(
            tenant(ORGANIZATION_A),
            validCommand()
        );

        assertEquals("INVITED", invitedUser.status());
        assertEquals(
            UserStatus.INVITED,
            findStoredUser(ORGANIZATION_A, invitedUser.userReference()).getStatus()
        );
    }

    @Test
    void suspendsAnActiveUserWithinTheProvidedTenant() {
        User activeUser = userRepository.create(User.restore(
            "USR-2026-3108-00000ZA7B902",
            ORGANIZATION_A,
            "active@example.com",
            "Active",
            "User",
            UserStatus.ACTIVE
        ));

        userLifecycle.suspend(
            tenant(ORGANIZATION_A),
            activeUser.getReference()
        );

        assertEquals(
            UserStatus.SUSPENDED,
            findStoredUser(ORGANIZATION_A, activeUser.getReference()).getStatus()
        );
    }

    @Test
    void deactivatesAUserWithinTheProvidedTenant() {
        UserLifecycleResult createdUser = userLifecycle.create(
            tenant(ORGANIZATION_A),
            validCommand()
        );

        userLifecycle.deactivate(
            tenant(ORGANIZATION_A),
            createdUser.userReference()
        );

        assertEquals(
            UserStatus.DEACTIVATED,
            findStoredUser(ORGANIZATION_A, createdUser.userReference()).getStatus()
        );
    }

    @Test
    void refusesToTransitionAUserOutsideTheProvidedTenant() {
        User activeUser = userRepository.create(User.restore(
            "USR-2026-3108-00000ZA7B903",
            ORGANIZATION_A,
            "active@example.com",
            "Active",
            "User",
            UserStatus.ACTIVE
        ));

        UserLifecycleException exception = assertThrows(
            UserLifecycleException.class,
            () -> userLifecycle.suspend(tenant(ORGANIZATION_B), activeUser.getReference())
        );

        assertEquals(UserLifecycleErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        assertEquals(
            UserStatus.ACTIVE,
            findStoredUser(ORGANIZATION_A, activeUser.getReference()).getStatus()
        );
    }

    @Test
    void allowsTheSameCanonicalEmailInDifferentOrganizations() {
        UserLifecycleResult userInOrganizationA = userLifecycle.create(
            tenant(ORGANIZATION_A),
            validCommand()
        );
        UserLifecycleResult userInOrganizationB = userLifecycle.create(
            tenant(ORGANIZATION_B),
            new UserProvisioningCommand(
                " ALICE.SMITH@EXAMPLE.COM ",
                "Alice",
                "Smith"
            )
        );

        assertEquals("alice.smith@example.com", userInOrganizationA.email());
        assertEquals("alice.smith@example.com", userInOrganizationB.email());
        assertEquals(2L, jpaUserRepository.count());
        assertEquals(
            List.of(ORGANIZATION_A, ORGANIZATION_B),
            jpaUserRepository.findAll()
                .stream()
                .map(UserEntity::getOrganizationReference)
                .sorted()
                .toList()
        );
    }

    private UserEntity findStoredUser(
        String organizationReference,
        String userReference
    ) {
        return jpaUserRepository.findByOrganizationReferenceAndReference(
            organizationReference,
            userReference
        ).orElseThrow();
    }

    private static Stream<UserProvisioningCommand> commandsWithBlankRequiredField() {
        return Stream.of(
            new UserProvisioningCommand(" ", "Alice", "Smith"),
            new UserProvisioningCommand("alice.smith@example.com", " ", "Smith"),
            new UserProvisioningCommand("alice.smith@example.com", "Alice", " ")
        );
    }

    private static TenantContext tenant(String organizationReference) {
        return new TenantContext(organizationReference);
    }

    private static UserProvisioningCommand validCommand() {
        return new UserProvisioningCommand("alice.smith@example.com", "Alice", "Smith");
    }
}
