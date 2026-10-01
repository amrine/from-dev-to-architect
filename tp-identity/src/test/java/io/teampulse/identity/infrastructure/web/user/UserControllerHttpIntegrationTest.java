package io.teampulse.identity.infrastructure.web.user;

import io.teampulse.common.error.ApiError;
import io.teampulse.identity.AbstractIdentityHttpIntegrationTest;
import io.teampulse.identity.IdentityTestApplication.DeterministicTenantContextProvider;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import io.teampulse.identity.infrastructure.persistence.repository.JpaUserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserControllerHttpIntegrationTest extends AbstractIdentityHttpIntegrationTest {

    private static final String TENANT_REFERENCE = "ORG-2026-0908-00000ZA7B900";
    private static final String OTHER_TENANT_REFERENCE = "ORG-2026-0908-00000ZA7B901";

    @Value("${local.server.port}")
    private int port;

    @Inject
    private DeterministicTenantContextProvider tenantContextProvider;

    @Inject
    private UserRepository userRepository;

    @Inject
    private JpaUserRepository jpaUserRepository;

    @BeforeEach
    void resetTenantProviderCalls() {
        tenantContextProvider.resetCalls();
    }

    @Test
    void createsAndPersistsAUserInTheProviderTenant() {
        var response = api().createUser(
            "alice@example.com",
            "Alice",
            "Smith"
        );
        var created = response.getBody();

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(created);
        assertEquals("CREATING", created.getStatus().getValue());
        assertEquals("alice@example.com", created.getEmail());
        assertEquals(1, tenantContextProvider.calls());

        UserEntity persisted = persisted(TENANT_REFERENCE, created.getReference());
        assertEquals(TENANT_REFERENCE, persisted.getOrganizationReference());
        assertEquals(UserStatus.CREATING, persisted.getStatus());
    }

    @Test
    void invitesAndPersistsAUserInInvitedStatus() {
        var response = api().inviteUser(
            "invited@example.com",
            "Invitee",
            "User"
        );
        var invited = response.getBody();

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(invited);
        assertEquals("INVITED", invited.getStatus().getValue());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(
            UserStatus.INVITED,
            persisted(TENANT_REFERENCE, invited.getReference()).getStatus()
        );
    }

    @Test
    void listsOnlyUsersInTheTenantResolvedByTheProvider() {
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

        var response = api().listUsers();
        List<org.openapitools.client.model.UserResponse> users = response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(users);
        assertEquals(List.of("USR-2026-1001-00000ZA7B900"), users.stream()
            .map(org.openapitools.client.model.UserResponse::getReference)
            .toList());
        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void ignoresAClientSuppliedOrganizationReferenceAndUsesTheProviderTenant() {
        var created = restTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "email": "forged-tenant@example.com",
                  "firstName": "Tenant",
                  "lastName": "Boundary",
                  "organizationReference": "ORG-2026-0908-00000ZA7B901"
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(UserResponse.class)
            .returnResult()
            .getResponseBody();

        assertEquals(1, tenantContextProvider.calls());
        assertNotNull(created);
        assertEquals(
            TENANT_REFERENCE,
            persisted(TENANT_REFERENCE, created.reference()).getOrganizationReference()
        );
        assertFalse(jpaUserRepository.findByOrganizationReferenceAndReference(
            OTHER_TENANT_REFERENCE,
            created.reference()
        ).isPresent());
    }

    @Test
    void mapsIdentityBusinessErrorsBeforeTheSharedFrameworkAdvice() {
        var created = api().createUser("duplicate@example.com", "First", "User");
        assertEquals(201, created.getStatusCode().value());

        ApiError error = restTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "email": "duplicate@example.com",
                  "firstName": "Second",
                  "lastName": "User"
                }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertEquals("USER_EMAIL_ALREADY_USED", error.code());
        assertEquals("Email is already used in this organization", error.message());
        assertFalse(error.message().contains("duplicate@example.com"));
        assertEquals(2, tenantContextProvider.calls());
        assertEquals(1L, jpaUserRepository.count());
    }

    @Test
    void returnsSanitizedSharedTransportErrorsForInvalidRequestStructure() {
        ApiError error = restTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "email": " ",
                  "firstName": "Alice",
                  "lastName": "Smith"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertEquals("VALIDATION_FAILED", error.code());
        assertEquals("Request validation failed", error.message());
        assertEquals(0, tenantContextProvider.calls());
        assertEquals(0L, jpaUserRepository.count());
    }

    private IdentityHttpDsl api() {
        return IdentityHttpDsl.runningAt(port);
    }

    private UserEntity persisted(String tenant, String reference) {
        return jpaUserRepository.findByOrganizationReferenceAndReference(tenant, reference)
            .orElseThrow();
    }

    private static User user(String reference, String tenant, String email) {
        return User.create(reference, tenant, email, "Test", "User");
    }
}
