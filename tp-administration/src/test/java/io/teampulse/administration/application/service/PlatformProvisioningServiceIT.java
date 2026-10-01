package io.teampulse.administration.application.service;

import io.teampulse.administration.AbstractAdministrationIntegrationTest;
import io.teampulse.administration.application.port.in.PlatformProvisioningCommand;
import io.teampulse.administration.application.port.in.PlatformProvisioningResult;
import io.teampulse.administration.application.port.in.PlatformProvisioningUseCase;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilitySnapshot;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlatformProvisioningServiceIT extends AbstractAdministrationIntegrationTest {

    @Inject
    private PlatformProvisioningUseCase platformProvisioning;

    @Inject
    private OrganizationResponsibilityDirectory organizationResponsibilities;

    @Inject
    private UserDirectory userDirectory;

    @Inject
    private UserRepository userRepository;

    @Test
    void createsAnOrganizationInCreatingAndItsInitialUserWithoutAssigningResponsibilities() {
        PlatformProvisioningResult result = platformProvisioning.createOrganizationAndInitialUser(
            command("TeamPulse create", "create@example.test")
        );

        assertProvisionedState(result, UserStatus.CREATING);
    }

    @Test
    void invitesTheInitialUserWithoutAcceptingTheInvitationOrActivatingTheOrganization() {
        PlatformProvisioningResult result = platformProvisioning.inviteInitialUser(
            command("TeamPulse invite", "invite@example.test")
        );

        assertProvisionedState(result, UserStatus.INVITED);
    }

    private void assertProvisionedState(
        PlatformProvisioningResult result,
        UserStatus expectedUserStatus
    ) {
        Optional<OrganizationResponsibilitySnapshot> organization =
            organizationResponsibilities.findByOrganizationReference(result.organizationReference());
        var user = userRepository.findByReference(
            result.organizationReference(),
            result.userReference()
        ).orElseThrow();

        assertEquals(OrganizationLifecycleState.CREATING, organization.orElseThrow().status());
        assertEquals(result.organizationReference(), organization.orElseThrow().organizationReference());
        assertEquals(expectedUserStatus, user.getStatus());
        assertEquals(
            UserAvailability.PENDING,
            userDirectory.check(result.organizationReference(), result.userReference())
        );
        assertNull(organization.orElseThrow().administratorReference());
        assertNull(organization.orElseThrow().managerReference());
    }

    private static PlatformProvisioningCommand command(String organizationName, String email) {
        return new PlatformProvisioningCommand(
            new OrganizationProvisioningCommand(organizationName, "Europe/Paris"),
            new UserProvisioningCommand(email, "Initial", "User")
        );
    }
}
