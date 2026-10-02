package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserLifecycleErrorCode;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.in.user.CreateUserCommand;
import io.teampulse.identity.application.port.in.user.UserLifecycleUseCase;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLifecycleContractServiceTest {

    private static final TenantContext TENANT =
        new TenantContext("ORG-2026-3008-00000ZA7B900");
    private static final String USER_REFERENCE = "USR-2026-3008-00000ZA7B900";

    @Mock
    private UserLifecycleUseCase userLifecycleUseCase;

    @InjectMocks
    private UserLifecycleContractService service;

    @Test
    void exposesCreationAsAReferenceResultThroughThePublicContract() {
        UserProvisioningCommand publicCommand = new UserProvisioningCommand(
            "alice@example.com",
            "Alice",
            "Smith"
        );
        when(userLifecycleUseCase.create(
            TENANT,
            new CreateUserCommand("alice@example.com", "Alice", "Smith")
        )).thenReturn(User.create(
            USER_REFERENCE,
            TENANT.tenantReference(),
            "alice@example.com",
            "Alice",
            "Smith"
        ));

        UserLifecycleResult result = service.create(TENANT, publicCommand);

        assertEquals(USER_REFERENCE, result.userReference());
        verify(userLifecycleUseCase).create(
            TENANT,
            new CreateUserCommand("alice@example.com", "Alice", "Smith")
        );
    }

    @Test
    void mapsNotFoundWithoutExposingTheInternalMessageAndPreservesTheCause() {
        UserException internalFailure = new UserException(
            UserErrorCode.NOT_FOUND,
            "User private@example.com was not found in organization"
        );
        when(userLifecycleUseCase.suspend(TENANT, USER_REFERENCE)).thenThrow(
            internalFailure
        );

        UserLifecycleException exception = assertThrows(
            UserLifecycleException.class,
            () -> service.suspend(TENANT, USER_REFERENCE)
        );

        assertEquals(UserLifecycleErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        assertEquals("User was not found", exception.getMessage());
        assertSame(internalFailure, exception.getCause());
    }

    @Test
    void mapsDuplicateEmailWithoutExposingTheInternalMessageAndPreservesTheCause() {
        UserProvisioningCommand command = new UserProvisioningCommand(
            "private@example.com",
            "Alice",
            "Smith"
        );
        UserException internalFailure = new UserException(
            UserErrorCode.EMAIL_ALREADY_USED,
            "Email private@example.com is already used in this organization"
        );
        when(userLifecycleUseCase.invite(
            TENANT,
            new CreateUserCommand("private@example.com", "Alice", "Smith")
        )).thenThrow(internalFailure);

        UserLifecycleException exception = assertThrows(
            UserLifecycleException.class,
            () -> service.invite(TENANT, command)
        );

        assertEquals(UserLifecycleErrorCode.EMAIL_ALREADY_USED, exception.getErrorCode());
        assertEquals("Email is already used in this organization", exception.getMessage());
        assertSame(internalFailure, exception.getCause());
    }

    @Test
    void mapsPersistenceFailuresToThePublicTechnicalCategoryAndPreservesTheCause() {
        DataAccessResourceFailureException persistenceFailure =
            new DataAccessResourceFailureException("Database is unavailable");
        when(userLifecycleUseCase.create(
            TENANT,
            new CreateUserCommand("alice@example.com", "Alice", "Smith")
        )).thenThrow(persistenceFailure);

        UserLifecycleException exception = assertThrows(
            UserLifecycleException.class,
            () -> service.create(
                TENANT,
                new UserProvisioningCommand("alice@example.com", "Alice", "Smith")
            )
        );

        assertEquals(UserLifecycleErrorCode.OPERATION_FAILED, exception.getErrorCode());
        assertEquals("User lifecycle operation failed", exception.getMessage());
        assertSame(persistenceFailure, exception.getCause());
    }

    @Test
    void preservesTheCauseChainWhenMappingAnInternalFailure() {
        IllegalStateException persistenceCause = new IllegalStateException("database conflict");
        UserException internalFailure = new UserException(
            UserErrorCode.CONCURRENT_MODIFICATION,
            "User was modified concurrently",
            persistenceCause
        );
        when(userLifecycleUseCase.suspend(TENANT, USER_REFERENCE))
            .thenThrow(internalFailure);

        UserLifecycleException exception = assertThrows(
            UserLifecycleException.class,
            () -> service.suspend(TENANT, USER_REFERENCE)
        );

        assertEquals(
            UserLifecycleErrorCode.CONCURRENT_MODIFICATION,
            exception.getErrorCode()
        );
        assertSame(internalFailure, exception.getCause());
        assertSame(persistenceCause, exception.getCause().getCause());
    }
}
