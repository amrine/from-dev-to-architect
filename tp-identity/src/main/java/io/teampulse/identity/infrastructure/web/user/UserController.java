package io.teampulse.identity.infrastructure.web.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.in.user.ListUsersUseCase;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/users")
@Tag(name = "Users", description = "Tenant-scoped user operations.")
public class UserController {

    private final UserLifecycle userLifecycle;
    private final ListUsersUseCase listUsersUseCase;
    private final TenantContextProvider tenantContextProvider;
    private final UserMapper userMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
        @Valid @RequestBody UserProvisioningCommand command
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        UserLifecycleResult user = userLifecycle.create(tenantContext, command);
        return userMapper.toResponse(user);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse invite(
        @Valid @RequestBody UserProvisioningCommand command
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        UserLifecycleResult user = userLifecycle.invite(tenantContext, command);
        return userMapper.toResponse(user);
    }

    @GetMapping
    public List<UserResponse> list() {
        TenantContext tenantContext = tenantContextProvider.current();
        return listUsersUseCase.list(tenantContext).stream()
            .map(userMapper::toResponse)
            .toList();
    }
}
