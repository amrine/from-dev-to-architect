package io.teampulse.administration.application.service;

import io.teampulse.administration.AbstractAdministrationIntegrationTest;
import io.teampulse.administration.application.error.PlatformAdministrationErrorCode;
import io.teampulse.administration.application.error.PlatformAdministrationException;
import io.teampulse.administration.application.port.in.UserLifecycleAdministrationUseCase;
import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.organization.application.port.in.organization.CreateOrganizationCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationLifecycleUseCase;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleUsersUseCase;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.application.port.in.team.TeamMemberCommand;
import io.teampulse.team.application.port.in.team.TeamMembershipUseCase;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserLifecycleAdministrationServiceIT extends AbstractAdministrationIntegrationTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1001-00000ZA7B901";
    private static final String TARGET_USER_REFERENCE = "USR-2026-1001-00000ZA7B901";
    private static final String OTHER_ADMIN_REFERENCE = "USR-2026-1001-00000ZA7B902";
    private static final String OTHER_MANAGER_REFERENCE = "USR-2026-1001-00000ZA7B903";

    @Inject
    private UserLifecycleAdministrationUseCase userLifecycleAdministration;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserDirectory userDirectory;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private OrganizationLifecycleUseCase organizationLifecycle;

    @Inject
    private OrganizationResponsibleUsersUseCase organizationResponsibleUsers;

    @Inject
    private TeamLifecycleUseCase teamLifecycle;

    @Inject
    private TeamMembershipUseCase teamMembership;

    @ParameterizedTest
    @EnumSource(value = OrganizationStatus.class, names = {"CREATING", "ACTIVE", "SUSPENDED"})
    void blocksSuspensionForResponsibilitiesInEveryNonArchivedOrganizationStatus(
        OrganizationStatus organizationStatus
    ) {
        String organizationReference = seedOrganizationWithResponsibility(organizationStatus);

        PlatformAdministrationException exception = assertThrows(
            PlatformAdministrationException.class,
            () -> userLifecycleAdministration.suspendUser(
                new TenantContext(organizationReference),
                TARGET_USER_REFERENCE
            )
        );

        assertEquals(
            PlatformAdministrationErrorCode.USER_HAS_ACTIVE_RESPONSIBILITIES,
            exception.getErrorCode()
        );
        assertUserAvailability(organizationReference, UserAvailability.AVAILABLE);
    }

    @ParameterizedTest
    @EnumSource(value = TeamStatus.class, names = {"ACTIVE", "SUSPENDED"})
    void blocksDeactivationForActiveAndSuspendedTeamResponsibilities(TeamStatus teamStatus) {
        seedActiveUsers(ORGANIZATION_REFERENCE);
        seedActiveOrganization();
        Team team = teamLifecycle.create(
            new TenantContext(ORGANIZATION_REFERENCE),
            new CreateTeamCommand("Engineering", TARGET_USER_REFERENCE, OTHER_MANAGER_REFERENCE)
        );
        if (teamStatus == TeamStatus.SUSPENDED) {
            teamLifecycle.suspend(new TenantContext(ORGANIZATION_REFERENCE), team.getReference());
        }

        PlatformAdministrationException exception = assertThrows(
            PlatformAdministrationException.class,
            () -> userLifecycleAdministration.deactivateUser(
                new TenantContext(ORGANIZATION_REFERENCE),
                TARGET_USER_REFERENCE
            )
        );

        assertEquals(
            PlatformAdministrationErrorCode.USER_HAS_ACTIVE_RESPONSIBILITIES,
            exception.getErrorCode()
        );
        assertUserAvailability(ORGANIZATION_REFERENCE, UserAvailability.AVAILABLE);
    }

    @Test
    void allowsSuspensionWhenTheOrganizationResponsibilityIsArchived() {
        seedActiveUsers(ORGANIZATION_REFERENCE);
        organizationRepository.create(Organization.restore(
            ORGANIZATION_REFERENCE,
            "Archived organization",
            "UTC",
            TARGET_USER_REFERENCE,
            OTHER_MANAGER_REFERENCE,
            OrganizationStatus.ARCHIVED
        ));

        userLifecycleAdministration.suspendUser(
            new TenantContext(ORGANIZATION_REFERENCE),
            TARGET_USER_REFERENCE
        );

        assertUserAvailability(ORGANIZATION_REFERENCE, UserAvailability.UNAVAILABLE);
    }

    @Test
    void allowsDeactivationWhenTheOnlyTeamResponsibilityIsArchived() {
        seedActiveUsers(ORGANIZATION_REFERENCE);
        seedActiveOrganization();
        Team team = teamLifecycle.create(
            new TenantContext(ORGANIZATION_REFERENCE),
            new CreateTeamCommand("Archived team", TARGET_USER_REFERENCE, OTHER_MANAGER_REFERENCE)
        );
        teamLifecycle.archive(new TenantContext(ORGANIZATION_REFERENCE), team.getReference());

        userLifecycleAdministration.deactivateUser(
            new TenantContext(ORGANIZATION_REFERENCE),
            TARGET_USER_REFERENCE
        );

        assertUserAvailability(ORGANIZATION_REFERENCE, UserAvailability.UNAVAILABLE);
    }

    @Test
    void allowsDeactivationForOrdinaryTeamMembershipWithoutResponsibility() {
        seedActiveUsers(ORGANIZATION_REFERENCE);
        seedActiveOrganization();
        Team team = teamLifecycle.create(
            new TenantContext(ORGANIZATION_REFERENCE),
            new CreateTeamCommand("Member team", OTHER_ADMIN_REFERENCE, OTHER_MANAGER_REFERENCE)
        );
        teamMembership.addMember(
            new TenantContext(ORGANIZATION_REFERENCE),
            new TeamMemberCommand(team.getReference(), TARGET_USER_REFERENCE)
        );

        userLifecycleAdministration.deactivateUser(
            new TenantContext(ORGANIZATION_REFERENCE),
            TARGET_USER_REFERENCE
        );

        assertUserAvailability(ORGANIZATION_REFERENCE, UserAvailability.UNAVAILABLE);
    }

    private void seedActiveUsers(String organizationReference) {
        userRepository.create(activeUser(TARGET_USER_REFERENCE, organizationReference, "target@example.test"));
        userRepository.create(activeUser(OTHER_ADMIN_REFERENCE, organizationReference, "admin@example.test"));
        userRepository.create(activeUser(OTHER_MANAGER_REFERENCE, organizationReference, "manager@example.test"));
    }

    private String seedOrganizationWithResponsibility(OrganizationStatus status) {
        if (status == OrganizationStatus.CREATING) {
            Organization created = organizationLifecycle.create(
                new CreateOrganizationCommand("Creating organization", "UTC")
            );
            userRepository.create(activeUser(
                TARGET_USER_REFERENCE,
                created.getReference(),
                "target@example.test"
            ));
            organizationResponsibleUsers.assignAdministrator(
                new OrganizationResponsibleCommand(created.getReference(), TARGET_USER_REFERENCE)
            );
            return created.getReference();
        }

        seedActiveUsers(ORGANIZATION_REFERENCE);
        organizationRepository.create(Organization.restore(
            ORGANIZATION_REFERENCE,
            "Organization responsibility",
            "UTC",
            TARGET_USER_REFERENCE,
            OTHER_MANAGER_REFERENCE,
            status
        ));
        return ORGANIZATION_REFERENCE;
    }

    private void seedActiveOrganization() {
        organizationRepository.create(Organization.restore(
            ORGANIZATION_REFERENCE,
            "Active organization",
            "UTC",
            OTHER_ADMIN_REFERENCE,
            OTHER_MANAGER_REFERENCE,
            OrganizationStatus.ACTIVE
        ));
    }

    private void assertUserAvailability(String organizationReference, UserAvailability expected) {
        assertEquals(
            expected,
            userDirectory.check(organizationReference, TARGET_USER_REFERENCE)
        );
    }

    private static User activeUser(String reference, String organizationReference, String email) {
        return User.restore(
            reference,
            organizationReference,
            email,
            "Test",
            "User",
            UserStatus.ACTIVE
        );
    }
}
