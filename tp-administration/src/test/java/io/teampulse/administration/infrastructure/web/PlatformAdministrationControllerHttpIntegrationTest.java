package io.teampulse.administration.infrastructure.web;

import io.teampulse.AdministrationTestApplication.DeterministicTenantContextProvider;
import io.teampulse.administration.AbstractAdministrationHttpIntegrationTest;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.error.ApiError;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilitySnapshot;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlatformAdministrationControllerHttpIntegrationTest
    extends AbstractAdministrationHttpIntegrationTest {

    private static final String TEST_TENANT_REFERENCE = "ORG-2026-1001-00000ZA7B900";

    @Value("${local.server.port}")
    private int port;

    @Inject
    private DeterministicTenantContextProvider tenantContextProvider;

    @Inject
    private UserLifecycle userLifecycle;

    @Inject
    private UserRepository userRepository;

    @Inject
    private OrganizationResponsibilityDirectory organizationResponsibilities;

    private PlatformAdministrationHttpDsl api;

    @BeforeEach
    void prepareClientAndTenantProvider() {
        api = PlatformAdministrationHttpDsl.runningAt(port);
        tenantContextProvider.resetCalls();
    }

    @Test
    void createsOrganizationAndInitialUserThroughRealModulesAndKeepsOrganizationCreating() {
        var response = api.createOrganizationAndInitialUser(
            "Platform create",
            "Europe/Paris",
            "create@example.test",
            "Initial",
            "User"
        );

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertProvisionedState(response.getBody().getOrganizationReference(),
            response.getBody().getUserReference(), UserStatus.CREATING);
    }

    @Test
    void invitesInitialUserWithoutAcceptingTheInvitationOrActivatingTheOrganization() {
        var response = api.inviteInitialUser(
            "Platform invite",
            "Europe/Paris",
            "invite@example.test",
            "Initial",
            "User"
        );

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertProvisionedState(response.getBody().getOrganizationReference(),
            response.getBody().getUserReference(), UserStatus.INVITED);
    }

    @Test
    void mapsProvisioningValidationErrorsWithoutReturningSubmittedData() {
        ApiError error = restTestClient.post()
            .uri("/api/platform/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "organizationName": "A valid name",
                  "timezone": " ",
                  "email": "private@example.test",
                  "firstName": "Initial",
                  "lastName": "User"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertEquals("VALIDATION_FAILED", error.code());
        assertEquals("Request validation failed", error.message());
    }

    @Test
    void tenantScopedLifecycleRoutesResolveTheProviderOnceAndRespectIdentityTransitions() {
        var user = userLifecycle.create(
            new TenantContext(TEST_TENANT_REFERENCE),
            new UserProvisioningCommand("lifecycle@example.test", "Lifecycle", "User")
        );

        tenantContextProvider.resetCalls();
        restTestClient.post()
            .uri("/api/platform/users/{userReference}/suspension", user.userReference())
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .value(error -> assertEquals("USER_TRANSITION_NOT_ALLOWED", error.code()));
        assertEquals(1, tenantContextProvider.calls());
        assertPersistedUserStatus(TEST_TENANT_REFERENCE, user.userReference(), UserStatus.CREATING);

        tenantContextProvider.resetCalls();
        var deactivation = api.deactivateUser(user.userReference());
        assertEquals(204, deactivation.getStatusCode().value());
        assertEquals(1, tenantContextProvider.calls());
        assertPersistedUserStatus(TEST_TENANT_REFERENCE, user.userReference(), UserStatus.DEACTIVATED);
    }

    private void assertProvisionedState(
        String organizationReference,
        String userReference,
        UserStatus expectedUserStatus
    ) {
        Optional<OrganizationResponsibilitySnapshot> organization =
            organizationResponsibilities.findByOrganizationReference(organizationReference);

        assertEquals(OrganizationLifecycleState.CREATING, organization.orElseThrow().status());
        assertEquals(organizationReference, organization.orElseThrow().organizationReference());
        assertNull(organization.orElseThrow().administratorReference());
        assertNull(organization.orElseThrow().managerReference());
        assertPersistedUserStatus(organizationReference, userReference, expectedUserStatus);
    }

    private void assertPersistedUserStatus(
        String organizationReference,
        String userReference,
        UserStatus expectedStatus
    ) {
        var user = userRepository.findByReference(organizationReference, userReference)
            .orElseThrow();
        assertEquals(expectedStatus, user.getStatus());
    }
}
