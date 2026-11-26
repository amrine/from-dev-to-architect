package io.teampulse.administration.infrastructure.web.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.teampulse.administration.application.port.in.identity.UserLifecycleAdministrationResult;
import io.teampulse.administration.application.port.in.identity.UserLifecycleAdministrationUseCase;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
    name = "Platform administration",
    description = "Platform-wide user provisioning and lifecycle operations."
)
@RequestMapping("/api/platform")
@AllArgsConstructor
public class UserLifecycleAdministrationController {

    private final UserLifecycleAdministrationUseCase userLifecycleAdministration;
    private final TenantContextProvider tenantContextProvider;
    private final UserLifecycleAdministrationMapper userLifecycleAdministrationMapper;

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        operationId = "createInitialUser",
        summary = "Create an initial user for the current platform tenant"
    )
    public UserLifecycleAdministrationResponse createInitialUser(
        @Valid @RequestBody UserLifecycleAdministrationRequest request
    ) {
        UserProvisioningCommand command = userLifecycleAdministrationMapper.toCommand(request);
        TenantContext tenantContext = tenantContextProvider.current();
        UserLifecycleAdministrationResult result = userLifecycleAdministration.createInitialUser(tenantContext, command);
        return userLifecycleAdministrationMapper.toResponse(result);
    }

    @PostMapping("/users/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        operationId = "inviteInitialUser",
        summary = "Invite an initial user to the current platform tenant"
    )
    public UserLifecycleAdministrationResponse inviteInitialUser(
        @Valid @RequestBody UserLifecycleAdministrationRequest request
    ) {
        UserProvisioningCommand command = userLifecycleAdministrationMapper.toCommand(request);
        TenantContext tenantContext = tenantContextProvider.current();
        UserLifecycleAdministrationResult result = userLifecycleAdministration.inviteInitialUser(tenantContext, command);
        return userLifecycleAdministrationMapper.toResponse(result);
    }

    @PostMapping("/users/{userReference}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "suspendUser", summary = "Suspend a user in the current platform tenant")
    public void suspendUser(@PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        userLifecycleAdministration.suspendUser(tenantContext, userReference);
    }

    @PostMapping("/users/{userReference}/deactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deactivateUser", summary = "Deactivate a user in the current platform tenant")
    public void deactivateUser(@PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        userLifecycleAdministration.deactivateUser(tenantContext, userReference);
    }

}
