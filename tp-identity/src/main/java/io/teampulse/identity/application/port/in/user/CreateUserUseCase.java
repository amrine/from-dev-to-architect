package io.teampulse.identity.application.port.in.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.domain.user.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreateUserUseCase {

    User create(
        @NotNull TenantContext tenantContext,
        @NotNull @Valid CreateUserCommand command
    );
}
