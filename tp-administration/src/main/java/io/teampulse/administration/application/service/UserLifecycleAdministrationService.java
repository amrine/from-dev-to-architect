package io.teampulse.administration.application.service;

import io.teampulse.administration.application.error.PlatformAdministrationErrorCode;
import io.teampulse.administration.application.error.PlatformAdministrationException;
import io.teampulse.administration.application.port.in.PlatformProvisioningResult;
import io.teampulse.administration.application.port.in.UserLifecycleAdministrationUseCase;
import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleResult;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.organization.api.organization.OrganizationResponsibilities;
import io.teampulse.team.api.TeamLifecycleState;
import io.teampulse.team.api.TeamResponsibility;
import io.teampulse.team.api.TeamResponsibilityDirectory;
import io.teampulse.team.api.TeamResponsibilityDirectoryException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

@Service
@Validated
@Transactional
@AllArgsConstructor
public class UserLifecycleAdministrationService implements UserLifecycleAdministrationUseCase {

    private final UserLifecycle userLifecycle;
    private final OrganizationDirectory organizationDirectory;
    private final TeamResponsibilityDirectory teamResponsibilityDirectory;

    @Override
    public PlatformProvisioningResult createInitialUser(
        TenantContext tenantContext,
        UserProvisioningCommand initialUser
    ) {
        return provisionInitialUser(tenantContext, initialUser, userLifecycle::create);
    }

    @Override
    public PlatformProvisioningResult inviteInitialUser(
        TenantContext tenantContext,
        UserProvisioningCommand initialUser
    ) {
        return provisionInitialUser(tenantContext, initialUser, userLifecycle::invite);
    }

    @Override
    public void suspendUser(TenantContext tenantContext, String userReference) {
        ensureNoActiveResponsibilities(tenantContext, userReference);
        executeUserLifecycle(() -> userLifecycle.suspend(tenantContext, userReference));
    }

    @Override
    public void deactivateUser(TenantContext tenantContext, String userReference) {
        ensureNoActiveResponsibilities(tenantContext, userReference);
        executeUserLifecycle(() -> userLifecycle.deactivate(tenantContext, userReference));
    }

    private PlatformProvisioningResult provisionInitialUser(
        TenantContext tenantContext,
        UserProvisioningCommand initialUser,
        BiFunction<TenantContext, UserProvisioningCommand, UserLifecycleResult> userOperation
    ) {
        ensureOrganizationExists(tenantContext);
        UserLifecycleResult user = runUserOperation(tenantContext, initialUser, userOperation);
        return new PlatformProvisioningResult(tenantContext.tenantReference(), user.userReference());
    }

    private void ensureOrganizationExists(TenantContext tenantContext) {
        try {
            organizationDirectory
                .findByOrganizationReference(tenantContext.tenantReference())
                .orElseThrow(() -> new PlatformAdministrationException(
                    PlatformAdministrationErrorCode.ORGANIZATION_NOT_FOUND
                ));
        } catch (OrganizationDirectoryException exception) {
            throw new PlatformAdministrationException(
                PlatformAdministrationErrorCode.ORGANIZATION_LOOKUP_UNAVAILABLE,
                exception
            );
        }
    }

    private UserLifecycleResult runUserOperation(
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

    private void ensureNoActiveResponsibilities(TenantContext tenantContext, String userReference) {
        Optional<OrganizationResponsibilities> organizationResponsibility;
        List<TeamResponsibility> teamResponsibilities;

        try {
            organizationResponsibility = organizationDirectory.findByOrganizationReference(
                tenantContext.tenantReference()
            );
            teamResponsibilities = teamResponsibilityDirectory.findByResponsibleUser(
                tenantContext,
                userReference
            );
        } catch (OrganizationDirectoryException | TeamResponsibilityDirectoryException exception) {
            throw new PlatformAdministrationException(PlatformAdministrationErrorCode.RESPONSIBILITY_CHECK_UNAVAILABLE, exception);
        }

        if (hasActiveOrganizationResponsibility(organizationResponsibility, userReference)
            || hasActiveTeamResponsibility(teamResponsibilities)) {
            throw new PlatformAdministrationException(PlatformAdministrationErrorCode.USER_HAS_ACTIVE_RESPONSIBILITIES);
        }
    }

    private static boolean hasActiveOrganizationResponsibility(
        Optional<OrganizationResponsibilities> responsibility,
        String userReference
    ) {
        return responsibility
            .filter(snapshot -> !OrganizationLifecycleResult.STATUS_ARCHIVED.equals(snapshot.status()))
            .filter(snapshot -> userReference.equals(snapshot.administratorReference())
                || userReference.equals(snapshot.managerReference()))
            .isPresent();
    }

    private static boolean hasActiveTeamResponsibility(List<TeamResponsibility> responsibilities) {
        return responsibilities.stream()
            .anyMatch(snapshot -> EnumSet.of(TeamLifecycleState.ACTIVE, TeamLifecycleState.SUSPENDED).contains(snapshot.status()));
    }

    private void executeUserLifecycle(Runnable lifecycleOperation) {
        try {
            lifecycleOperation.run();
        } catch (UserLifecycleException exception) {
            throw mapUserFailure(exception);
        }
    }

    private static PlatformAdministrationException mapUserFailure(UserLifecycleException exception) {
        PlatformAdministrationErrorCode platformAdministrationErrorCode = switch (exception.getErrorCode()) {
            case USER_NOT_FOUND -> PlatformAdministrationErrorCode.USER_NOT_FOUND;
            case TRANSITION_NOT_ALLOWED -> PlatformAdministrationErrorCode.USER_TRANSITION_NOT_ALLOWED;
            case EMAIL_ALREADY_USED -> PlatformAdministrationErrorCode.USER_EMAIL_ALREADY_USED;
            case INVALID_USER_DATA -> PlatformAdministrationErrorCode.USER_DATA_INVALID;
            case CONCURRENT_MODIFICATION, OPERATION_FAILED -> PlatformAdministrationErrorCode.USER_LIFECYCLE_FAILED;
        };
        return new PlatformAdministrationException(platformAdministrationErrorCode, exception);
    }
}
