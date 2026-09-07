package io.teampulse.identity.infrastructure.persistence.repository;

import io.teampulse.identity.AbstractIntegrationTest;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.testsupport.persistence.MutableAuditDateTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaUserRepositoryAdapterIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_A =
        "ORG-2026-2808-00000ZA7B900";
    private static final String ORGANIZATION_B =
        "ORG-2026-2808-00000ZA7B901";

    private static final String USER_REFERENCE_1 =
        "USR-2026-2808-00000ZA7B900";
    private static final String USER_REFERENCE_2 =
        "USR-2026-2808-00000ZA7B901";
    private static final String USER_REFERENCE_3 =
        "USR-2026-2808-00000ZA7B902";

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-28T08:00:00Z");
    private static final Instant MODIFIED_AT =
        Instant.parse("2026-08-28T09:30:00Z");

    @Autowired
    private JpaUserRepositoryAdapter userRepository;

    @Autowired
    private JpaUserRepository jpaRepository;

    @Autowired
    private MutableAuditDateTimeProvider auditDateTimeProvider;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(jpaRepository::deleteAllInBatch);
        auditDateTimeProvider.setCurrentInstant(CREATED_AT);
    }

    @Nested
    class MappingTests {

        @Test
        void persistsAndRestoresTheCompleteBusinessState() {
            // GIVEN
            User invitedUser = User.invite(
                USER_REFERENCE_1,
                ORGANIZATION_A,
                " Alice.Smith@Example.COM ",
                " Alice ",
                " Smith "
            );

            // WHEN
            User createdUser = userRepository.create(invitedUser);

            // THEN
            User restoredUser = userRepository.findByReference(ORGANIZATION_A, USER_REFERENCE_1).orElseThrow();
            UserEntity storedEntity = findEntity(ORGANIZATION_A, USER_REFERENCE_1);

            assertUserState(createdUser, UserStatus.INVITED);
            assertUserState(restoredUser, UserStatus.INVITED);
            assertNotNull(storedEntity.getId());
            assertEquals(USER_REFERENCE_1, storedEntity.getReference());
            assertEquals(ORGANIZATION_A, storedEntity.getOrganizationReference());
            assertEquals("alice.smith@example.com", storedEntity.getEmail());
            assertEquals("Alice", storedEntity.getFirstName());
            assertEquals("Smith", storedEntity.getLastName());
            assertEquals(UserStatus.INVITED, storedEntity.getStatus());
        }
    }

    @Nested
    class UniquenessTests {

        @Test
        void enforcesCanonicalEmailUniquenessWithinOneOrganizationOnly() {
            // GIVEN
            userRepository.create(createUser(
                USER_REFERENCE_1,
                ORGANIZATION_A,
                "shared@example.com"
            ));

            // WHEN / THEN
            UserException exception = assertThrows(
                UserException.class,
                () -> userRepository.create(createUser(USER_REFERENCE_2, ORGANIZATION_A, " SHARED@EXAMPLE.COM "))
            );

            assertEquals(UserErrorCode.EMAIL_ALREADY_USED, exception.getErrorCode());

            User userInOtherOrganization = userRepository.create(createUser(
                USER_REFERENCE_3,
                ORGANIZATION_B,
                "shared@example.com"
            ));

            assertEquals(ORGANIZATION_B, userInOtherOrganization.getOrganizationReference());
        }

        @Test
        void rejectsAReferenceCollisionAcrossOrganizationsWithoutChangingTheExistingUser() {
            // GIVEN
            userRepository.create(createUser(
                USER_REFERENCE_1,
                ORGANIZATION_A,
                "alice@example.com"
            ));
            Long originalId = findEntity(ORGANIZATION_A, USER_REFERENCE_1).getId();

            // WHEN / THEN
            UserException exception = assertThrows(
                UserException.class,
                () -> userRepository.create(createUser(
                    USER_REFERENCE_1,
                    ORGANIZATION_B,
                    "bob@example.com"
                ))
            );

            assertEquals(
                UserErrorCode.REFERENCE_GENERATION_FAILED,
                exception.getErrorCode()
            );
            assertEquals(1L, jpaRepository.count());
            UserEntity originalEntity = findEntity(ORGANIZATION_A, USER_REFERENCE_1);
            assertEquals(originalId, originalEntity.getId());
            assertEquals("alice@example.com", originalEntity.getEmail());
            assertTrue(userRepository.findByReference(ORGANIZATION_B, USER_REFERENCE_1).isEmpty());
        }
    }

    @Nested
    class TenantIsolationTests {

        @Test
        void rejectsNullOrganizationReferenceInPostgreSql() {
            // Bypass domain validation so PostgreSQL enforces the NOT NULL constraint.
            UserEntity entity = new UserEntity()
                .setReference(USER_REFERENCE_1)
                .setOrganizationReference(null)
                .setEmail("alice@example.com")
                .setFirstName("Alice")
                .setLastName("Smith")
                .setStatus(UserStatus.CREATING);

            DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> inTransactionTemplate(() -> jpaRepository.save(entity))
            );

            PSQLException databaseException = assertInstanceOf(
                PSQLException.class,
                exception.getMostSpecificCause()
            );
            assertEquals("23502", databaseException.getSQLState());
            assertNotNull(databaseException.getServerErrorMessage());
            assertEquals(
                "organization_reference",
                databaseException.getServerErrorMessage().getColumn()
            );
            assertEquals(0L, jpaRepository.count());
        }

        @Test
        void scopesReadsExistenceChecksAndUpdatesToTheOrganization() {
            // GIVEN
            userRepository.create(createUser(
                USER_REFERENCE_1,
                ORGANIZATION_A,
                "alice@example.com"
            ));
            userRepository.create(createUser(
                USER_REFERENCE_2,
                ORGANIZATION_B,
                "bob@example.com"
            ));

            // WHEN / THEN
            assertTrue(userRepository.findByReference(
                ORGANIZATION_A,
                USER_REFERENCE_1
            ).isPresent());
            assertFalse(userRepository.findByReference(
                ORGANIZATION_B,
                USER_REFERENCE_1
            ).isPresent());
            assertEquals(
                List.of(USER_REFERENCE_1),
                userRepository.findAll(ORGANIZATION_A)
                    .stream()
                    .map(User::getReference)
                    .toList()
            );
            assertEquals(
                List.of(USER_REFERENCE_2),
                userRepository.findAll(ORGANIZATION_B)
                    .stream()
                    .map(User::getReference)
                    .toList()
            );
            assertTrue(userRepository.existsByEmail(
                ORGANIZATION_A,
                "alice@example.com"
            ));
            assertFalse(userRepository.existsByEmail(
                ORGANIZATION_B,
                "alice@example.com"
            ));

            User wrongTenantUpdate = User.restore(
                USER_REFERENCE_1,
                ORGANIZATION_B,
                "alice@example.com",
                "Alice",
                "Updated",
                UserStatus.CREATING
            );
            UserException exception = assertThrows(
                UserException.class,
                () -> userRepository.update(wrongTenantUpdate)
            );

            assertEquals(UserErrorCode.NOT_FOUND, exception.getErrorCode());
            assertEquals(
                "Smith",
                userRepository.findByReference(ORGANIZATION_A, USER_REFERENCE_1)
                    .orElseThrow()
                    .getLastName()
            );
        }
    }

    @Nested
    class AuditTests {

        @Test
        void populatesAuditFieldsAndPreservesCreationAuditOnUpdate() {
            // GIVEN / WHEN
            User user = userRepository.create(createUser(
                USER_REFERENCE_1,
                ORGANIZATION_A,
                "alice@example.com"
            ));

            // THEN
            UserEntity createdEntity = findEntity(
                ORGANIZATION_A,
                USER_REFERENCE_1
            );

            assertNotNull(createdEntity.getId());
            assertEquals(0L, createdEntity.getVersion());
            assertEquals("SYSTEM", createdEntity.getCreatedBy());
            assertEquals(CREATED_AT, createdEntity.getCreatedAt());
            assertEquals("SYSTEM", createdEntity.getModifiedBy());
            assertEquals(CREATED_AT, createdEntity.getModifiedAt());

            auditDateTimeProvider.setCurrentInstant(MODIFIED_AT);
            user.activate();
            userRepository.update(user);

            UserEntity updatedEntity = findEntity(
                ORGANIZATION_A,
                USER_REFERENCE_1
            );

            assertEquals(createdEntity.getId(), updatedEntity.getId());
            assertEquals(1L, updatedEntity.getVersion());
            assertEquals("SYSTEM", updatedEntity.getCreatedBy());
            assertEquals(CREATED_AT, updatedEntity.getCreatedAt());
            assertEquals("SYSTEM", updatedEntity.getModifiedBy());
            assertEquals(MODIFIED_AT, updatedEntity.getModifiedAt());
            assertEquals(UserStatus.ACTIVE, updatedEntity.getStatus());
        }
    }

    @Nested
    class OptimisticLockingTests {

        @Test
        void translatesAnOptimisticLockConflictWithoutOverwritingTheCommittedUpdate() {
            userRepository.create(createUser(
                USER_REFERENCE_1,
                ORGANIZATION_A,
                "alice@example.com"
            ));

            TransactionTemplate independentTransaction = new TransactionTemplate(
                transactionTemplate.getTransactionManager()
            );
            independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            UserException exception = assertThrows(
                UserException.class,
                () -> transactionTemplate.executeWithoutResult(ignored -> {
                    UserEntity staleEntity = findEntity(ORGANIZATION_A, USER_REFERENCE_1);
                    assertEquals(0L, staleEntity.getVersion());

                    // Commit an independent update while the first persistence context
                    // retains version 0. This makes the conflict deterministic.
                    independentTransaction.executeWithoutResult(independentStatus -> {
                        UserEntity currentEntity = findEntity(ORGANIZATION_A, USER_REFERENCE_1);
                        assertNotSame(staleEntity, currentEntity);
                        currentEntity.setLastName("First update");
                        jpaRepository.save(currentEntity);
                    });

                    userRepository.update(User.restore(
                        USER_REFERENCE_1,
                        ORGANIZATION_A,
                        "alice@example.com",
                        "Alice",
                        "Stale update",
                        UserStatus.CREATING
                    ));
                })
            );

            assertEquals(UserErrorCode.CONCURRENT_MODIFICATION, exception.getErrorCode());
            assertInstanceOf(OptimisticLockingFailureException.class, exception.getCause());
            UserEntity committedEntity = findEntity(ORGANIZATION_A, USER_REFERENCE_1);
            assertEquals("First update", committedEntity.getLastName());
            assertEquals(1L, committedEntity.getVersion());
        }
    }

    /**
     * Finds a UserEntity by organization reference and user reference.
     *
     * @param organizationReference The reference of the organization.
     * @param userReference The reference of the user.
     * @return The found UserEntity.
     * @throws NoSuchElementException if no UserEntity is found.
     */
    private UserEntity findEntity(
        String organizationReference,
        String userReference
    ) {
        return jpaRepository.findByOrganizationReferenceAndReference(
            organizationReference,
            userReference
        ).orElseThrow();
    }

    /**
     * Creates a new User.
     *
     * @param userReference The reference of the user.
     * @param organizationReference The reference of the organization.
     * @param email The email of the user.
     * @return The created User.
     */
    private static User createUser(
        String userReference,
        String organizationReference,
        String email
    ) {
        return User.create(
            userReference,
            organizationReference,
            email,
            "Alice",
            "Smith"
        );
    }

    /**
     * Asserts the state of a User object.
     *
     * @param user The User object to assert.
     * @param expectedStatus The expected status of the User.
     */
    private static void assertUserState(User user, UserStatus expectedStatus) {
        assertEquals(USER_REFERENCE_1, user.getReference());
        assertEquals(ORGANIZATION_A, user.getOrganizationReference());
        assertEquals("alice.smith@example.com", user.getEmail());
        assertEquals("Alice", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals(expectedStatus, user.getStatus());
    }
}
