package io.teampulse.identity.infrastructure.web.user;

import io.teampulse.common.error.ApiError;
import io.teampulse.identity.AbstractIdentityIT;
import io.teampulse.identity.IdentityTestApplication.DeterministicTenantContextProvider;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.identity.events.UserInvited;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.identity.infrastructure.persistence.repository.JpaUserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class UserControllerIT extends AbstractIdentityIT {

    private static final String TENANT_REFERENCE = "ORG-2026-0908-00000ZA7B900";
    private static final String OTHER_TENANT_REFERENCE = "ORG-2026-0908-00000ZA7B901";

    @Inject
    private DeterministicTenantContextProvider tenantContextProvider;

    @Inject
    private UserRepository userRepository;

    @Inject
    private JpaUserRepository jpaUserRepository;

    @Inject
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void resetTenantProviderCalls() {
        tenantContextProvider.resetCalls();
        inTransactionTemplate(jpaUserRepository::deleteAllInBatch);
    }

    @Test
    void createsAndPersistsAUserInTheProviderTenant() throws Exception {
        UserProvisioningCommand command = new UserProvisioningCommand(
            "alice@example.com", "Alice", "Smith"
        );

        MvcResult result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATING"))
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andReturn();
        UserResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserResponse.class
        );

        assertEquals(1, tenantContextProvider.calls());
        UserEntity persisted = persisted(TENANT_REFERENCE, response.reference());
        assertEquals(TENANT_REFERENCE, persisted.getOrganizationReference());
        assertEquals(UserStatus.CREATING, persisted.getStatus());
    }

    @Test
    void invitesAndPersistsAUserInInvitedStatus() throws Exception {
        UserProvisioningCommand command = new UserProvisioningCommand(
            "invited@example.com", "Invitee", "User"
        );

        MvcResult result = mockMvc.perform(post("/api/users/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("INVITED"))
            .andReturn();

        UserResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserResponse.class
        );

        assertEquals(1, tenantContextProvider.calls());
        assertEquals(UserStatus.INVITED, persisted(TENANT_REFERENCE, response.reference()).getStatus());
        assertEquals(
            List.of(new UserInvited(TENANT_REFERENCE, response.reference())),
            applicationEvents.stream(UserInvited.class).toList()
        );
    }

    @Test
    void listsOnlyUsersInTheTenantResolvedByTheProvider() throws Exception {
        userRepository.create(user(
            "USR-2026-1001-00000ZA7B900",
            TENANT_REFERENCE,
            "tenant@example.com"
        ));
        userRepository.create(user(
            "USR-2026-1001-00000ZA7B901",
            OTHER_TENANT_REFERENCE,
            "other@example.com"
        ));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].reference").value("USR-2026-1001-00000ZA7B900"));

        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void mapsIdentityBusinessErrorsBeforeTheSharedFrameworkAdvice() throws Exception {

        UserProvisioningCommand firstCommand = new UserProvisioningCommand(
            "duplicate@example.com", "First", "User");
        UserProvisioningCommand secondCommand = new UserProvisioningCommand(
            "duplicate@example.com", "Second", "User");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstCommand)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCommand)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_USED"))
            .andExpect(jsonPath("$.message").value("Email is already used in this organization"))
            .andReturn();


        ApiError response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            ApiError.class
        );
        assertEquals("USER_EMAIL_ALREADY_USED", response.code());
        assertEquals("Email is already used in this organization", response.message());
        assertEquals(2, tenantContextProvider.calls());
        assertEquals(1L, jpaUserRepository.count());
    }

    @Test
    void keepsTheIdentityFieldErrorForInvalidBusinessEmail() throws Exception {
        UserProvisioningCommand command = new UserProvisioningCommand(
            "not-an-email", "Alice", "Smith");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("INVALID_USER_EMAIL"))
            .andExpect(jsonPath("$.message").value("Email is invalid"));

        assertEquals(1, tenantContextProvider.calls());
        assertEquals(0L, jpaUserRepository.count());
    }

    @Test
    void returnsSanitizedSharedTransportErrorsForInvalidRequestStructure() throws Exception {
        UserProvisioningCommand command = new UserProvisioningCommand(
            "  ", "Alice", "Smith");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"));

        assertEquals(0, tenantContextProvider.calls());
        assertEquals(0L, jpaUserRepository.count());
    }

    private UserEntity persisted(String tenant, String reference) {
        return jpaUserRepository.findByOrganizationReferenceAndReference(tenant, reference)
            .orElseThrow();
    }

    private static User user(String reference, String tenant, String email) {
        return User.create(reference, tenant, email, "Test", "User");
    }
}
