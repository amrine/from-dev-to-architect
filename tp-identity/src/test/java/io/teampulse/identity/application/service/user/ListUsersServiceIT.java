package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.AbstractIntegrationTest;
import io.teampulse.identity.application.port.in.user.ListUsersUseCase;
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
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(JpaTransactionManagerProbeConfiguration.class)
class ListUsersServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_A =
        "ORG-2026-3108-00000ZA7B910";
    private static final String ORGANIZATION_B =
        "ORG-2026-3108-00000ZA7B911";

    private static final String USER_REFERENCE_1 =
        "USR-2026-3108-00000ZA7B910";
    private static final String USER_REFERENCE_2 =
        "USR-2026-3108-00000ZA7B911";
    private static final String USER_REFERENCE_3 =
        "USR-2026-3108-00000ZA7B912";

    @Inject
    private ListUsersUseCase listUsersUseCase;

    @Inject
    private JpaUserRepository jpaUserRepository;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaUserRepository::deleteAllInBatch);
    }

    @Test
    void rejectsNullTenantContext() {
        assertThrows(
            ConstraintViolationException.class,
            () -> listUsersUseCase.list(null)
        );
    }

    @Test
    void listsOnlyUsersBelongingToTheRequestedOrganization() {
        // GIVEN
        inTransactionTemplate(() -> jpaUserRepository.saveAll(List.of(
            userEntity(
                USER_REFERENCE_1,
                ORGANIZATION_A,
                "alice@example.com",
                "Alice"
            ),
            userEntity(
                USER_REFERENCE_2,
                ORGANIZATION_A,
                "bob@example.com",
                "Bob"
            ),
            userEntity(
                USER_REFERENCE_3,
                ORGANIZATION_B,
                "charlie@example.com",
                "Charlie"
            )
        )));

        // WHEN
        List<User> users = listUsersUseCase.list(
            new TenantContext(ORGANIZATION_A)
        );

        // THEN
        assertEquals(
            List.of(USER_REFERENCE_1, USER_REFERENCE_2),
            users.stream()
                .map(User::getReference)
                .sorted()
                .toList()
        );
        assertEquals(
            List.of(ORGANIZATION_A),
            users.stream()
                .map(User::getOrganizationReference)
                .distinct()
                .toList()
        );
    }

    @Test
    void listsUsersInsideAnActiveReadOnlyTransaction() {
        listUsersUseCase.list(new TenantContext(ORGANIZATION_A));

        TransactionManagerProbe.TransactionObservation observation =
            transactionProbe.observation();
        assertTrue(
            observation.name().endsWith("ListUsersService.list")
        );
        assertTrue(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    private static UserEntity userEntity(
        String reference,
        String organizationReference,
        String email,
        String firstName
    ) {
        return new UserEntity()
            .setReference(reference)
            .setOrganizationReference(organizationReference)
            .setEmail(email)
            .setFirstName(firstName)
            .setLastName("User")
            .setStatus(UserStatus.CREATING);
    }
}
