package io.teampulse.identity.infrastructure.persistence.repository;

import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryAdapterTest {

    @Mock(mockMaker = MockMakers.PROXY)
    private JpaUserRepository jpaRepository;

    @Mock(mockMaker = MockMakers.PROXY)
    private UserPersistenceMapper mapper;

    @InjectMocks
    private JpaUserRepositoryAdapter adapter;

    @Test
    void translatesAReferenceCollisionWithoutRetrying() {
        // GIVEN
        User user = User.create(
            "USR-2026-2808-00000ZA7B900",
            "ORG-2026-2808-00000ZA7B900",
            "alice@example.com",
            "Alice",
            "Smith"
        );
        UserEntity entity = new UserEntity();
        DataIntegrityViolationException persistenceFailure =
            new DataIntegrityViolationException(
                "Could not insert user",
                new ConstraintViolationException(
                    "Duplicate reference",
                    new SQLException("Duplicate reference", "23505"),
                    "uk_users_reference"
                )
            );
        when(mapper.toEntity(user)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(any(UserEntity.class)))
            .thenThrow(persistenceFailure);

        // WHEN
        UserException exception = assertThrows(
            UserException.class,
            () -> adapter.create(user)
        );

        // THEN
        assertEquals(UserErrorCode.REFERENCE_GENERATION_FAILED, exception.getErrorCode());
        assertSame(persistenceFailure, exception.getCause());
        ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(jpaRepository, times(1)).saveAndFlush(entityCaptor.capture());
        assertSame(entity, entityCaptor.getValue());
        verifyNoMoreInteractions(jpaRepository);
    }
}
