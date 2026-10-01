package io.teampulse.administration.application.service;

import io.teampulse.administration.application.error.PlatformAdministrationErrorCode;
import io.teampulse.administration.application.error.PlatformAdministrationException;
import io.teampulse.administration.application.port.in.UserLifecycleAdministrationUseCase;
import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectoryException;
import io.teampulse.organization.api.organization.OrganizationResponsibilitySnapshot;
import io.teampulse.team.api.TeamLifecycleState;
import io.teampulse.team.api.TeamResponsibilityDirectory;
import io.teampulse.team.api.TeamResponsibilityDirectoryException;
import io.teampulse.team.api.TeamResponsibilitySnapshot;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
@AllArgsConstructor
public class UserLifecycleAdministrationService implements UserLifecycleAdministrationUseCase {

    private final UserLifecycle userLifecycle;
    private final OrganizationResponsibilityDirectory organizationResponsibilityDirectory;
    private final TeamResponsibilityDirectory teamResponsibilityDirectory;

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

    private void ensureNoActiveResponsibilities(TenantContext tenantContext, String userReference) {
        Optional<OrganizationResponsibilitySnapshot> organizationResponsibility;
        List<TeamResponsibilitySnapshot> teamResponsibilities;

        try {
            organizationResponsibility = organizationResponsibilityDirectory.findByOrganizationReference(
                tenantContext.tenantReference()
            );
            teamResponsibilities = teamResponsibilityDirectory.findByResponsibleUser(
                tenantContext,
                userReference
            );
        } catch (OrganizationResponsibilityDirectoryException | TeamResponsibilityDirectoryException exception) {
            throw new PlatformAdministrationException(
                PlatformAdministrationErrorCode.RESPONSIBILITY_CHECK_UNAVAILABLE,
                "User responsibilities could not be checked",
                exception
            );
        }

        if (hasActiveOrganizationResponsibility(organizationResponsibility, userReference)
            || hasActiveTeamResponsibility(teamResponsibilities)) {
            throw new PlatformAdministrationException(
                PlatformAdministrationErrorCode.USER_HAS_ACTIVE_RESPONSIBILITIES,
                "User has active responsibilities"
            );
        }
    }

    private static boolean hasActiveOrganizationResponsibility(
        Optional<OrganizationResponsibilitySnapshot> responsibility,
        String userReference
    ) {
        return responsibility
            .filter(snapshot -> isActiveOrganizationStatus(snapshot.status()))
            .filter(snapshot -> userReference.equals(snapshot.administratorReference())
                || userReference.equals(snapshot.managerReference()))
            .isPresent();
    }

    private static boolean isActiveOrganizationStatus(OrganizationLifecycleState status) {
        return switch (status) {
            case CREATING, ACTIVE, SUSPENDED -> true;
            case ARCHIVED -> false;
        };
    }

    private static boolean hasActiveTeamResponsibility(List<TeamResponsibilitySnapshot> responsibilities) {
        return responsibilities.stream()
            .anyMatch(snapshot -> isActiveTeamStatus(snapshot.status()));
    }

    private static boolean isActiveTeamStatus(TeamLifecycleState status) {
        return switch (status) {
            case ACTIVE, SUSPENDED -> true;
            case ARCHIVED -> false;
        };
    }

    private void executeUserLifecycle(Runnable lifecycleOperation) {
        try {
            lifecycleOperation.run();
        } catch (UserLifecycleException exception) {
            throw mapUserFailure(exception);
        }
    }

    private static PlatformAdministrationException mapUserFailure(UserLifecycleException exception) {
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
            case EMAIL_ALREADY_USED, INVALID_USER_DATA -> new PlatformAdministrationException(
                PlatformAdministrationErrorCode.USER_LIFECYCLE_FAILED,
                "User lifecycle operation failed",
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
