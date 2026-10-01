package io.teampulse.administration.application.service;

import io.teampulse.administration.application.error.PlatformAdministrationErrorCode;
import io.teampulse.administration.application.error.PlatformAdministrationException;
import io.teampulse.administration.application.port.in.PlatformProvisioningCommand;
import io.teampulse.administration.application.port.in.PlatformProvisioningResult;
import io.teampulse.administration.application.port.in.PlatformProvisioningUseCase;
import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationProvisioning;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationProvisioningErrorCode;
import io.teampulse.organization.api.organization.OrganizationProvisioningException;
import io.teampulse.organization.api.organization.OrganizationProvisioningResult;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.function.BiFunction;

@Service
@Validated
@AllArgsConstructor
public class PlatformProvisioningService implements PlatformProvisioningUseCase {

    private final OrganizationProvisioning organizationProvisioning;
    private final UserLifecycle userLifecycle;

    @Override
    public PlatformProvisioningResult createOrganizationAndInitialUser(
        PlatformProvisioningCommand command
    ) {
        return provision(command, userLifecycle::create);
    }

    @Override
    public PlatformProvisioningResult inviteInitialUser(PlatformProvisioningCommand command) {
        return provision(command, userLifecycle::invite);
    }

    private PlatformProvisioningResult provision(
        PlatformProvisioningCommand command,
        BiFunction<TenantContext, UserProvisioningCommand, UserLifecycleResult> userOperation
    ) {
        OrganizationProvisioningResult organization = createOrganization(command.organization());
        if (organization.status() != OrganizationLifecycleState.CREATING) {
            throw new PlatformAdministrationException(
                PlatformAdministrationErrorCode.ORGANIZATION_MUST_REMAIN_CREATING,
                "Organization provisioning must remain in CREATING"
            );
        }

        TenantContext tenantContext = new TenantContext(organization.organizationReference());
        UserLifecycleResult user = createInitialUser(
            tenantContext,
            command.initialUser(),
            userOperation
        );

        return new PlatformProvisioningResult(
            organization.organizationReference(),
            user.userReference()
        );
    }

    private OrganizationProvisioningResult createOrganization(
        OrganizationProvisioningCommand command
    ) {
        try {
            return organizationProvisioning.createOrganization(command);
        } catch (OrganizationProvisioningException exception) {
            throw mapOrganizationFailure(exception);
        }
    }

    private UserLifecycleResult createInitialUser(
        TenantContext tenantContext,
        UserProvisioningCommand command,
        BiFunction<TenantContext, UserProvisioningCommand, UserLifecycleResult> userOperation
    ) {
        try {
            return userOperation.apply(tenantContext, command);
        } catch (UserLifecycleException exception) {
            throw mapUserFailure(exception);
        }
    }

    private static PlatformAdministrationException mapOrganizationFailure(
        OrganizationProvisioningException exception
    ) {
        return switch (exception.getErrorCode()) {
            case INVALID_NAME, INVALID_TIMEZONE -> new PlatformAdministrationException(
                PlatformAdministrationErrorCode.ORGANIZATION_DATA_INVALID,
                "Organization data is invalid",
                exception
            );
            case REFERENCE_GENERATION_FAILED, CONCURRENT_MODIFICATION, OPERATION_FAILED ->
                new PlatformAdministrationException(
                    PlatformAdministrationErrorCode.ORGANIZATION_PROVISIONING_FAILED,
                    "Organization provisioning failed",
                    exception
                );
        };
    }

    private static PlatformAdministrationException mapUserFailure(
        UserLifecycleException exception
    ) {
        return switch (exception.getErrorCode()) {
            case USER_NOT_FOUND -> new PlatformAdministrationException(
                PlatformAdministrationErrorCode.USER_NOT_FOUND,
                "User was not found",
                exception
            );
            case TRANSITION_NOT_ALLOWED -> new PlatformAdministrationException(
                PlatformAdministrationErrorCode.USER_TRANSITION_NOT_ALLOWED,
                "User transition is not allowed",
                exception
            );
            case EMAIL_ALREADY_USED -> new PlatformAdministrationException(
                PlatformAdministrationErrorCode.USER_EMAIL_ALREADY_USED,
                "Email is already used in this organization",
                exception
            );
            case INVALID_USER_DATA -> new PlatformAdministrationException(
                PlatformAdministrationErrorCode.USER_DATA_INVALID,
                "User data is invalid",
                exception
            );
            case CONCURRENT_MODIFICATION, OPERATION_FAILED -> new PlatformAdministrationException(
                PlatformAdministrationErrorCode.USER_LIFECYCLE_FAILED,
                "User lifecycle operation failed",
                exception
            );
        };
    }
}
