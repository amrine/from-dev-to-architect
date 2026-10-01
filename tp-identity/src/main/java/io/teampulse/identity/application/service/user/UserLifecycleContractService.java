package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserLifecycleErrorCode;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.in.user.CreateUserCommand;
import io.teampulse.identity.application.port.in.user.UserLifecycleUseCase;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.function.Supplier;

@Service
@Validated
@AllArgsConstructor
public class UserLifecycleContractService implements UserLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        UserLifecycleContractService.class
    );

    private final UserLifecycleUseCase userLifecycleUseCase;

    @Override
    public UserLifecycleResult create(
        TenantContext tenantContext,
        UserProvisioningCommand command
    ) {
        return mapPublicFailure(() -> new UserLifecycleResult(
            userLifecycleUseCase.create(tenantContext, command(command)).getReference()
        ));
    }

    @Override
    public UserLifecycleResult invite(
        TenantContext tenantContext,
        UserProvisioningCommand command
    ) {
        return mapPublicFailure(() -> new UserLifecycleResult(
            userLifecycleUseCase.invite(tenantContext, command(command)).getReference()
        ));
    }

    @Override
    public void suspend(TenantContext tenantContext, String userReference) {
        mapPublicFailure(() -> {
            userLifecycleUseCase.suspend(tenantContext, userReference);
            return null;
        });
    }

    @Override
    public void deactivate(TenantContext tenantContext, String userReference) {
        mapPublicFailure(() -> {
            userLifecycleUseCase.deactivate(tenantContext, userReference);
            return null;
        });
    }

    private <T> T mapPublicFailure(Supplier<T> command) {
        try {
            return command.get();
        } catch (UserException exception) {
            UserLifecycleException publicFailure = toPublicFailure(exception);
            if (
                publicFailure.getErrorCode()
                    == UserLifecycleErrorCode.CONCURRENT_MODIFICATION
            ) {
                LOGGER.warn("User lifecycle operation conflicted with a concurrent update", exception);
            } else if (publicFailure.getErrorCode() == UserLifecycleErrorCode.OPERATION_FAILED) {
                LOGGER.error("User lifecycle operation failed", exception);
            }
            throw publicFailure;
        }
    }

    private static CreateUserCommand command(UserProvisioningCommand command) {
        return new CreateUserCommand(
            command.email(),
            command.firstName(),
            command.lastName()
        );
    }

    private static UserLifecycleException toPublicFailure(UserException exception) {
        UserLifecycleErrorCode publicCode = switch (exception.getErrorCode()) {
            case NOT_FOUND -> UserLifecycleErrorCode.USER_NOT_FOUND;
            case INVALID_STATUS_TRANSITION -> UserLifecycleErrorCode.TRANSITION_NOT_ALLOWED;
            case EMAIL_ALREADY_USED -> UserLifecycleErrorCode.EMAIL_ALREADY_USED;
            case INVALID_EMAIL, INVALID_FIRST_NAME, INVALID_LAST_NAME ->
                UserLifecycleErrorCode.INVALID_USER_DATA;
            case CONCURRENT_MODIFICATION -> UserLifecycleErrorCode.CONCURRENT_MODIFICATION;
            default -> UserLifecycleErrorCode.OPERATION_FAILED;
        };
        String safeMessage = switch (publicCode) {
            case USER_NOT_FOUND -> "User was not found";
            case TRANSITION_NOT_ALLOWED -> "User transition is not allowed";
            case EMAIL_ALREADY_USED -> "Email is already used in this organization";
            case INVALID_USER_DATA -> "User data is invalid";
            case CONCURRENT_MODIFICATION -> "User changed during the operation";
            case OPERATION_FAILED -> "User lifecycle operation failed";
        };
        return new UserLifecycleException(publicCode, safeMessage);
    }
}
