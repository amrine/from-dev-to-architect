package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.AbstractIntegrationTest;
import io.teampulse.identity.application.port.in.user.CreateUserCommand;
import io.teampulse.identity.application.port.in.user.CreateUserUseCase;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.identity.infrastructure.persistence.repository.JpaUserRepository;
import io.teampulse.testsupport.transaction.JpaTransactionManagerProbeConfiguration;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(JpaTransactionManagerProbeConfiguration.class)
class CreateUserServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_A =
        "ORG-2026-3108-00000ZA7B900";
    private static final String ORGANIZATION_B =
        "ORG-2026-3108-00000ZA7B901";

    @Inject
    private CreateUserUseCase createUserUseCase;

    @Inject
    private JpaUserRepository jpaUserRepository;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaUserRepository::deleteAllInBatch);
    }

    @Test
    void rejectsNullTenantContextBeforePersisting() {
        assertThrows(
            ConstraintViolationException.class,
            () -> createUserUseCase.create(null, validCommand())
        );

        assertEquals(0L, jpaUserRepository.count());
    }

    @Test
    void rejectsNullCommandBeforePersisting() {
        assertThrows(
            ConstraintViolationException.class,
            () -> createUserUseCase.create(tenant(ORGANIZATION_A), null)
        );

        assertEquals(0L, jpaUserRepository.count());
    }

    @ParameterizedTest
    @MethodSource("commandsWithBlankRequiredField")
    void rejectsBlankCommandFieldsBeforePersisting(CreateUserCommand command) {
        assertThrows(
            ConstraintViolationException.class,
            () -> createUserUseCase.create(tenant(ORGANIZATION_A), command)
        );

        assertEquals(0L, jpaUserRepository.count());
    }

    @Test
    void createsAndReturnsThePersistedUser() {
        User createdUser = createUserUseCase.create(
            tenant(ORGANIZATION_A),
            new CreateUserCommand(
                " Alice.Smith@Example.COM ",
                " Alice ",
                " Smith "
            )
        );

        assertTrue(createdUser.getReference().matches(
            "USR-[0-9]{4}-[0-9]{4}-[0-9A-Z]{12}"
        ));
        assertEquals(ORGANIZATION_A, createdUser.getOrganizationReference());
        assertEquals("alice.smith@example.com", createdUser.getEmail());
        assertEquals("Alice", createdUser.getFirstName());
        assertEquals("Smith", createdUser.getLastName());
        assertEquals(UserStatus.CREATING, createdUser.getStatus());

        UserEntity storedUser = findStoredUser(
            ORGANIZATION_A,
            createdUser.getReference()
        );
        assertEquals(createdUser.getReference(), storedUser.getReference());
        assertEquals(ORGANIZATION_A, storedUser.getOrganizationReference());
        assertEquals("alice.smith@example.com", storedUser.getEmail());
        assertEquals("Alice", storedUser.getFirstName());
        assertEquals("Smith", storedUser.getLastName());
        assertEquals(UserStatus.CREATING, storedUser.getStatus());
    }

    @Test
    void createsUserInsideAnActiveReadWriteTransaction() {
        createUserUseCase.create(
            tenant(ORGANIZATION_A),
            validCommand()
        );

        TransactionManagerProbe.TransactionObservation observation =
            transactionProbe.observation();
        assertTrue(
            observation.name().endsWith("CreateUserService.create")
        );
        assertFalse(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    @Test
    void rejectsCanonicalEmailAlreadyUsedInTheSameOrganization() {
        createUserUseCase.create(
            tenant(ORGANIZATION_A),
            validCommand()
        );

        UserException exception = assertThrows(
            UserException.class,
            () -> createUserUseCase.create(
                tenant(ORGANIZATION_A),
                new CreateUserCommand(
                    " ALICE.SMITH@EXAMPLE.COM ",
                    "Another",
                    "User"
                )
            )
        );

        assertEquals(UserErrorCode.EMAIL_ALREADY_USED, exception.getErrorCode());
        assertEquals(1L, jpaUserRepository.count());
    }

    @Test
    void allowsTheSameCanonicalEmailInDifferentOrganizations() {
        User userInOrganizationA = createUserUseCase.create(
            tenant(ORGANIZATION_A),
            validCommand()
        );
        User userInOrganizationB = createUserUseCase.create(
            tenant(ORGANIZATION_B),
            new CreateUserCommand(
                " ALICE.SMITH@EXAMPLE.COM ",
                "Alice",
                "Smith"
            )
        );

        assertEquals(ORGANIZATION_A, userInOrganizationA.getOrganizationReference());
        assertEquals(ORGANIZATION_B, userInOrganizationB.getOrganizationReference());
        assertEquals("alice.smith@example.com", userInOrganizationA.getEmail());
        assertEquals("alice.smith@example.com", userInOrganizationB.getEmail());
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

    private static Stream<CreateUserCommand> commandsWithBlankRequiredField() {
        return Stream.of(
            new CreateUserCommand(" ", "Alice", "Smith"),
            new CreateUserCommand("alice.smith@example.com", " ", "Smith"),
            new CreateUserCommand("alice.smith@example.com", "Alice", " ")
        );
    }

    private static TenantContext tenant(String organizationReference) {
        return new TenantContext(organizationReference);
    }

    private static CreateUserCommand validCommand() {
        return new CreateUserCommand(
            "alice.smith@example.com",
            "Alice",
            "Smith"
        );
    }
}
