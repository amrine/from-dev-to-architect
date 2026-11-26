package io.teampulse.administration.infrastructure.web.identity;

import io.teampulse.AbstractAdministrationMvcIT;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.testsupport.context.DeterministicTenantContextProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserLifecycleAdministrationControllerIT extends AbstractAdministrationMvcIT {

    private static final String TENANT_REFERENCE = "ORG-2026-1001-00000ZA7B900";
    private static final String TARGET_USER_REFERENCE = "USR-2026-1001-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-1001-00000ZA7B901";

    @Value("${teampulse.openapi.info-version}")
    private String expectedApiVersion;

    @Inject
    private DeterministicTenantContextProvider tenantContextProvider;

    @Inject
    private UserRepository userRepository;

    @Inject
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void resetTenantProviderCalls() {
        tenantContextProvider.resetCalls();
    }

    @Test
    void createsAndPersistsAnInitialUserInTheProviderTenant() throws Exception {
        seedOrganization();

        UserLifecycleAdministrationResponse response = provision(
            "/api/platform/users",
            "created@example.test"
        );

        assertEquals(TENANT_REFERENCE, response.organizationReference());
        assertEquals(
            UserStatus.CREATING,
            persistedUser(response.userReference()).getStatus()
        );
    }

    @Test
    void invitesAndPersistsAnInitialUserInInvitedStatus() throws Exception {
        seedOrganization();

        UserLifecycleAdministrationResponse response = provision(
            "/api/platform/users/invitations",
            "invited@example.test"
        );

        assertEquals(TENANT_REFERENCE, response.organizationReference());
        assertEquals(
            UserStatus.INVITED,
            persistedUser(response.userReference()).getStatus()
        );
    }

    @Test
    void suspendsAnUnassignedUserThroughTheHttpEndpoint() throws Exception {
        seedOrganization();
        userRepository.create(activeUser(TARGET_USER_REFERENCE, "suspend@example.test"));

        mockMvc.perform(post("/api/platform/users/{userReference}/suspension", TARGET_USER_REFERENCE))
            .andExpect(status().isNoContent());

        assertEquals(UserStatus.SUSPENDED, persistedUser(TARGET_USER_REFERENCE).getStatus());
        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void deactivatesAnUnassignedUserThroughTheHttpEndpoint() throws Exception {
        seedOrganization();
        userRepository.create(activeUser(TARGET_USER_REFERENCE, "deactivate@example.test"));

        mockMvc.perform(post("/api/platform/users/{userReference}/deactivation", TARGET_USER_REFERENCE))
            .andExpect(status().isNoContent());

        assertEquals(UserStatus.DEACTIVATED, persistedUser(TARGET_USER_REFERENCE).getStatus());
        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void mapsAnUnknownOrganizationToANotFoundResponseWithoutCreatingAUser() throws Exception {
        mockMvc.perform(post("/api/platform/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request("unknown-organization@example.test"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Organization was not found"));

        assertEquals(1, tenantContextProvider.calls());
        assertTrue(userRepository.findAll(TENANT_REFERENCE).isEmpty());
    }

    @Test
    void mapsInvalidUserDataToASanitizedBusinessError() throws Exception {
        seedOrganization();

        mockMvc.perform(post("/api/platform/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request("not-an-email"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_USER_DATA"))
            .andExpect(jsonPath("$.message").value("User data is invalid"));

        assertEquals(1, tenantContextProvider.calls());
        assertTrue(userRepository.findAll(TENANT_REFERENCE).isEmpty());
    }

    @Test
    void rejectsInvalidRequestStructureBeforeResolvingTheTenant() throws Exception {
        mockMvc.perform(post("/api/platform/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UserLifecycleAdministrationRequest(" ", "Initial", "User")
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"));

        assertEquals(0, tenantContextProvider.calls());
        assertTrue(userRepository.findAll(TENANT_REFERENCE).isEmpty());
    }

    @Test
    void rejectsMalformedJsonBeforeResolvingTheTenant() throws Exception {
        mockMvc.perform(post("/api/platform/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("Request body is invalid"));

        assertEquals(0, tenantContextProvider.calls());
        assertTrue(userRepository.findAll(TENANT_REFERENCE).isEmpty());
    }

    @Test
    void refusesSuspensionWhileTheUserHasAnOrganizationResponsibility() throws Exception {
        seedOrganizationWithAdministrator();

        mockMvc.perform(post("/api/platform/users/{userReference}/suspension", TARGET_USER_REFERENCE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USER_HAS_ACTIVE_RESPONSIBILITIES"))
            .andExpect(jsonPath("$.message").value("User has active responsibilities"));

        assertEquals(UserStatus.ACTIVE, persistedUser(TARGET_USER_REFERENCE).getStatus());
        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void mapsAnUnknownUserToNotFoundForLifecycleOperations() throws Exception {
        mockMvc.perform(post("/api/platform/users/{userReference}/suspension", TARGET_USER_REFERENCE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User was not found"));

        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void documentsApiMetadataAndAdministrationRoutes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("TeamPulse Platform Administration API"))
            .andExpect(jsonPath("$.info.version").value(expectedApiVersion))
            .andExpect(jsonPath("$.servers[0].url").value("/"))
            .andExpect(jsonPath("$.paths['/api/platform/users'].post.operationId").value("createInitialUser"))
            .andExpect(
                jsonPath("$.paths['/api/platform/users/invitations'].post.operationId")
                    .value("inviteInitialUser")
            );
    }

    private UserLifecycleAdministrationResponse provision(String uri, String email) throws Exception {
        MvcResult result = mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(email))))
            .andExpect(status().isCreated())
            .andReturn();
        UserLifecycleAdministrationResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserLifecycleAdministrationResponse.class
        );

        assertNotNull(response);
        assertEquals(1, tenantContextProvider.calls());
        return response;
    }

    private void seedOrganization() {
        organizationRepository.create(Organization.create(
            TENANT_REFERENCE,
            "Administration HTTP organization",
            "UTC"
        ));
    }

    private void seedOrganizationWithAdministrator() {
        organizationRepository.create(Organization.restore(
            TENANT_REFERENCE,
            "Administration HTTP organization with administrator",
            "UTC",
            TARGET_USER_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.ACTIVE
        ));
        userRepository.create(activeUser(TARGET_USER_REFERENCE, "administrator@example.test"));
        userRepository.create(activeUser(MANAGER_REFERENCE, "manager@example.test"));
    }

    private User persistedUser(String reference) {
        return userRepository.findByReference(TENANT_REFERENCE, reference).orElseThrow();
    }

    private static UserLifecycleAdministrationRequest request(String email) {
        return new UserLifecycleAdministrationRequest(email, "Initial", "User");
    }

    private static User activeUser(String reference, String email) {
        return User.restore(
            reference,
            TENANT_REFERENCE,
            email,
            "Test",
            "User",
            UserStatus.ACTIVE
        );
    }
}
