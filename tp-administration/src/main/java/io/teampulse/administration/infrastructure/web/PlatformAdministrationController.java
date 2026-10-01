package io.teampulse.administration.infrastructure.web;

import io.teampulse.administration.application.port.in.PlatformProvisioningCommand;
import io.teampulse.administration.application.port.in.PlatformProvisioningResult;
import io.teampulse.administration.application.port.in.PlatformProvisioningUseCase;
import io.teampulse.administration.application.port.in.UserLifecycleAdministrationUseCase;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@Profile({"local", "development"})
@AllArgsConstructor
public class PlatformAdministrationController {

    private final PlatformProvisioningUseCase platformProvisioning;
    private final UserLifecycleAdministrationUseCase userLifecycleAdministration;
    private final TenantContextProvider tenantContextProvider;

    @PostMapping("/organizations")
    public ResponseEntity<PlatformProvisioningResponse> createOrganizationAndInitialUser(
        @Valid @RequestBody PlatformProvisioningRequest request
    ) {
        PlatformProvisioningResult result = platformProvisioning.createOrganizationAndInitialUser(
            command(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response(result));
    }

    @PostMapping("/organizations/invitations")
    public ResponseEntity<PlatformProvisioningResponse> inviteInitialUser(
        @Valid @RequestBody PlatformProvisioningRequest request
    ) {
        PlatformProvisioningResult result = platformProvisioning.inviteInitialUser(command(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response(result));
    }

    @PostMapping("/users/{userReference}/suspension")
    public ResponseEntity<Void> suspendUser(@PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        userLifecycleAdministration.suspendUser(tenantContext, userReference);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userReference}/deactivation")
    public ResponseEntity<Void> deactivateUser(@PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        userLifecycleAdministration.deactivateUser(tenantContext, userReference);
        return ResponseEntity.noContent().build();
    }

    private static PlatformProvisioningCommand command(PlatformProvisioningRequest request) {
        return new PlatformProvisioningCommand(
            new OrganizationProvisioningCommand(request.organizationName(), request.timezone()),
            new UserProvisioningCommand(request.email(), request.firstName(), request.lastName())
        );
    }

    private static PlatformProvisioningResponse response(PlatformProvisioningResult result) {
        return new PlatformProvisioningResponse(
            result.organizationReference(),
            result.userReference()
        );
    }
}
