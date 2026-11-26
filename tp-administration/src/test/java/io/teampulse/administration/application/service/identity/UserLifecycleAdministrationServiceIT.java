package io.teampulse.administration.application.service.identity;

import io.teampulse.AbstractAdministrationJpaIT;
import io.teampulse.AdministrationTestApplication;
import io.teampulse.administration.application.error.PlatformAdministrationErrorCode;
import io.teampulse.administration.application.error.PlatformAdministrationException;
import io.teampulse.administration.application.port.in.identity.UserLifecycleAdministrationResult;
import io.teampulse.administration.application.port.in.identity.UserLifecycleAdministrationUseCase;
import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycle;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleResult;
import io.teampulse.organization.api.lifecycle.OrganizationProvisioningCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleUsersUseCase;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.application.port.in.teammember.TeamMemberCommand;
import io.teampulse.team.application.port.in.teammember.TeamMembershipUseCase;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLifecycleAdministrationServiceIT extends AbstractAdministrationJpaIT {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1001-00000ZA7B901";
    private static final String TARGET_USER_REFERENCE = "USR-2026-1001-00000ZA7B901";
    private static final String OTHER_ADMIN_REFERENCE = "USR-2026-1001-00000ZA7B902";
    private static final String OTHER_MANAGER_REFERENCE = "USR-2026-1001-00000ZA7B903";
    private static final String UNKNOWN_ORGANIZATION_REFERENCE = "ORG-2026-1001-00000ZA7B999";

    @Inject
    private UserLifecycleAdministrationUseCase userLifecycleAdministration;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserDirectory userDirectory;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private OrganizationLifecycle organizationLifecycle;

    @Inject
    private OrganizationResponsibleUsersUseCase organizationResponsibleUsers;

    @Inject
    private TeamLifecycleUseCase teamLifecycle;

    @Inject
    private TeamMembershipUseCase teamMembership;

    @Test
    void createsAnInitialUserForAnExistingOrganizationWithoutAssigningResponsibilities() {
        TenantContext tenantContext = createOrganizationWithoutUsers("TeamPulse create");

        UserLifecycleAdministrationResult result = userLifecycleAdministration.createInitialUser(
            tenantContext,
            command("create@example.test")
        );

        assertInitialUserState(tenantContext, result, UserStatus.CREATING);
    }

    @Test
    void invitesAnInitialUserForAnExistingOrganizationWithoutAcceptingTheInvitation() {
        TenantContext tenantContext = createOrganizationWithoutUsers("TeamPulse invite");

        UserLifecycleAdministrationResult result = userLifecycleAdministration.inviteInitialUser(
            tenantContext,
            command("invite@example.test")
        );

        assertInitialUserState(tenantContext, result, UserStatus.INVITED);
    }

    @Test
    void leavesTheExistingOrganizationInPlaceWhenInitialUserCreationFails() {
        TenantContext tenantContext = createOrganizationWithoutUsers("Organization without user");

        PlatformAdministrationException exception = assertThrows(
            PlatformAdministrationException.class,
            () -> userLifecycleAdministration.createInitialUser(
                tenantContext,
                command("invalid-email")
            )
        );

        assertEquals(PlatformAdministrationErrorCode.USER_DATA_INVALID, exception.getErrorCode());
        assertTrue(organizationRepository.findByReference(tenantContext.tenantReference()).isPresent());
        assertTrue(userRepository.findAll(tenantContext.tenantReference()).isEmpty());
    }

    @Test
    void refusesToCreateAnInitialUserForAnUnknownOrganization() {
        TenantContext tenantContext = new TenantContext(UNKNOWN_ORGANIZATION_REFERENCE);

        PlatformAdministrationException exception = assertThrows(
            PlatformAdministrationException.class,
            () -> userLifecycleAdministration.createInitialUser(
                tenantContext,
                command("create@example.test")
            )
        );

        assertEquals(PlatformAdministrationErrorCode.ORGANIZATION_NOT_FOUND, exception.getErrorCode());
        assertTrue(userRepository.findAll(tenantContext.tenantReference()).isEmpty());
    }

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

    private TenantContext createOrganizationWithoutUsers(String organizationName) {
        OrganizationLifecycleResult organization = organizationLifecycle.create(
            new OrganizationProvisioningCommand(organizationName, "Europe/Paris")
        );

        assertEquals(OrganizationLifecycleResult.STATUS_CREATING, organization.status());
        assertTrue(userRepository.findAll(organization.organizationReference()).isEmpty());
        return new TenantContext(organization.organizationReference());
    }

    private void assertInitialUserState(
        TenantContext tenantContext,
        UserLifecycleAdministrationResult result,
        UserStatus expectedUserStatus
    ) {
        assertEquals(tenantContext.tenantReference(), result.organizationReference());
        Organization organization = organizationRepository.findByReference(
            result.organizationReference()
        ).orElseThrow();
        User user = userRepository.findByReference(
            result.organizationReference(),
            result.userReference()
        ).orElseThrow();

        assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        assertEquals(expectedUserStatus, user.getStatus());
        assertEquals(
            UserAvailability.PENDING,
            userDirectory.check(result.organizationReference(), result.userReference())
        );
        assertNull(organization.getAdminReference());
        assertNull(organization.getManagerReference());
    }

    private static UserProvisioningCommand command(String email) {
        return new UserProvisioningCommand(email, "Initial", "User");
    }

    private String seedOrganizationWithResponsibility(OrganizationStatus status) {
        if (status == OrganizationStatus.CREATING) {
            OrganizationLifecycleResult created = organizationLifecycle.create(
                new OrganizationProvisioningCommand("Creating organization", "UTC")
            );
            userRepository.create(activeUser(
                TARGET_USER_REFERENCE,
                created.organizationReference(),
                "target@example.test"
            ));
            organizationResponsibleUsers.assignAdministrator(
                new OrganizationResponsibleCommand(created.organizationReference(), TARGET_USER_REFERENCE)
            );
            return created.organizationReference();
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
