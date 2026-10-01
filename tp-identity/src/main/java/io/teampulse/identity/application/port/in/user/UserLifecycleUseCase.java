package io.teampulse.identity.application.port.in.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.domain.user.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UserLifecycleUseCase {

    User create(
        @NotNull TenantContext tenantContext,
        @NotNull @Valid CreateUserCommand command
    );

    User invite(
        @NotNull TenantContext tenantContext,
        @NotNull @Valid CreateUserCommand command
    );

    User suspend(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );

    User deactivate(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );
}
