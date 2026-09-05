package io.teampulse.identity.infrastructure.persistence.repository;

import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_organization_email";
    private static final String REFERENCE_UNIQUE_CONSTRAINT = "uk_users_reference";

    private final JpaUserRepository jpaUserRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    @Override
    public User create(User user) {
        try {
            // Flush before leaving the adapter so database constraint violations
            // can still be translated into domain errors within this boundary.
            return userPersistenceMapper.toDomain(jpaUserRepository.saveAndFlush(
                userPersistenceMapper.toEntity(user)
            ));
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    @Override
    public User update(User user) {
        try{
            UserEntity userEntity = jpaUserRepository.findByOrganizationReferenceAndReference(user.getOrganizationReference(), user.getReference())
                .orElseThrow(() -> new UserException(
                    UserErrorCode.NOT_FOUND,
                    "User %s was not found in organization %s".formatted(user.getReference(), user.getOrganizationReference())
                ));
            userPersistenceMapper.updateEntity(userEntity, user);
            // Flush before leaving the adapter so optimistic locking and database
            // constraint failures can still be translated within this boundary.
            return userPersistenceMapper.toDomain(jpaUserRepository.saveAndFlush(userEntity));
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new UserException(UserErrorCode.CONCURRENT_MODIFICATION, "User was modified concurrently", exception);
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw translatePersistenceException(exception);
        }
    }

    @Override
    public Optional<User> findByReference(String organizationReference, String userReference) {
        return jpaUserRepository.findByOrganizationReferenceAndReference(organizationReference, userReference)
            .map(userPersistenceMapper::toDomain);
    }

    @Override
    public List<User> findAll(String organizationReference) {
        return jpaUserRepository.findAllByOrganizationReference(organizationReference)
            .stream()
            .map(userPersistenceMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByEmail(String organizationReference, String email) {
        return jpaUserRepository.existsByOrganizationReferenceAndEmail(organizationReference, email);
    }

    private RuntimeException translatePersistenceException(RuntimeException exception) {
        String constraintName = findConstraintName(exception);

        if (constraintName != null) {
            if (constraintName.contains(EMAIL_UNIQUE_CONSTRAINT)) {
                return new UserException(UserErrorCode.EMAIL_ALREADY_USED, "Email is already used in this organization", exception);
            }

            if (constraintName.contains(REFERENCE_UNIQUE_CONSTRAINT)) {
                return new UserException(UserErrorCode.REFERENCE_GENERATION_FAILED, "Generated user reference already exists", exception);
            }
        }

        return exception;
    }

    private String findConstraintName(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            current = current.getCause();
        }

        return null;
    }
}
