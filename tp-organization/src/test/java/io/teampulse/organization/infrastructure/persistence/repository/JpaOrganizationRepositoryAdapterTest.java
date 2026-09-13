package io.teampulse.organization.infrastructure.persistence.repository;

import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.organization.infrastructure.persistence.mapper.OrganizationPersistenceMapper;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaOrganizationRepositoryAdapterTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1309-00000ZA7B900";

    @Mock(mockMaker = MockMakers.PROXY)
    private JpaOrganizationRepository jpaRepository;

    @Mock(mockMaker = MockMakers.PROXY)
    private OrganizationPersistenceMapper mapper;

    @InjectMocks
    private JpaOrganizationRepositoryAdapter adapter;

    @Test
    void translatesAReferenceCollisionWithoutRetrying() {
        Organization organization = organization();
        OrganizationEntity entity = new OrganizationEntity();
        DataIntegrityViolationException persistenceFailure =
            constraintViolation("uk_organizations_reference");
        when(mapper.toEntity(organization)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(any(OrganizationEntity.class)))
            .thenThrow(persistenceFailure);

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> adapter.create(organization)
        );

        assertEquals(
            OrganizationErrorCode.REFERENCE_GENERATION_FAILED,
            exception.getErrorCode()
        );
        assertSame(persistenceFailure, exception.getCause());
        ArgumentCaptor<OrganizationEntity> entityCaptor =
            ArgumentCaptor.forClass(OrganizationEntity.class);
        verify(jpaRepository, times(1)).saveAndFlush(entityCaptor.capture());
        assertSame(entity, entityCaptor.getValue());
        verifyNoMoreInteractions(jpaRepository);
    }

    @Test
    void propagatesAnUnknownConstraintViolation() {
        Organization organization = organization();
        OrganizationEntity entity = new OrganizationEntity();
        DataIntegrityViolationException persistenceFailure =
            constraintViolation("backup_uk_organizations_reference");
        when(mapper.toEntity(organization)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(entity))
            .thenThrow(persistenceFailure);

        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> adapter.create(organization)
        );

        assertSame(persistenceFailure, exception);
        verify(jpaRepository, times(1)).saveAndFlush(entity);
        verifyNoMoreInteractions(jpaRepository);
    }

    @ParameterizedTest
    @MethodSource("optimisticLockFailures")
    void translatesOptimisticLockFailures(RuntimeException persistenceFailure) {
        Organization organization = organization();
        OrganizationEntity entity = new OrganizationEntity();
        when(jpaRepository.findByReference(ORGANIZATION_REFERENCE))
            .thenReturn(Optional.of(entity));
        when(jpaRepository.saveAndFlush(entity))
            .thenThrow(persistenceFailure);

        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> adapter.update(organization)
        );

        assertEquals(
            OrganizationErrorCode.CONCURRENT_MODIFICATION,
            exception.getErrorCode()
        );
        assertSame(persistenceFailure, exception.getCause());
    }

    private static Stream<Arguments> optimisticLockFailures() {
        return Stream.of(
            Arguments.of(new OptimisticLockException("Stale organization")),
            Arguments.of(
                new OptimisticLockingFailureException("Stale organization")
            )
        );
    }

    private static Organization organization() {
        return Organization.create(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            "Europe/Paris"
        );
    }

    private static DataIntegrityViolationException constraintViolation(
        String constraintName
    ) {
        return new DataIntegrityViolationException(
            "Could not insert organization",
            new ConstraintViolationException(
                "Duplicate organization reference",
                new SQLException("Duplicate organization reference", "23505"),
                constraintName
            )
        );
    }
}
