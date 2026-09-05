package io.teampulse.identity.application.port.in.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.domain.user.model.User;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface ListUsersUseCase {

    List<User> list(@NotNull TenantContext tenantContext);
}
