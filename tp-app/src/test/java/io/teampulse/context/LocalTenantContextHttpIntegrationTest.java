package io.teampulse.context;

import io.teampulse.AbstractTpAppHttpIntegrationTest;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.error.ApiError;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilitySnapshot;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalTenantContextHttpIntegrationTest extends AbstractTpAppHttpIntegrationTest {

    @Inject
    private TenantContextProvider tenantContextProvider;

    @Inject
    private OrganizationResponsibilityDirectory organizationResponsibilities;

    @Test
    void keepsTheLocalDemoOrganizationCreatingAndRejectsTeamProvisioning() {
        restTestClient.get()
            .uri("/api/users")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("[]");

        TenantContext tenantContext = tenantContextProvider.current();
        Optional<OrganizationResponsibilitySnapshot> organization =
            organizationResponsibilities.findByOrganizationReference(
                tenantContext.tenantReference()
            );

        assertEquals(
            OrganizationLifecycleState.CREATING,
            organization.orElseThrow().status()
        );
        assertEquals(
            tenantContext.tenantReference(),
            organization.orElseThrow().organizationReference()
        );

        var result = restTestClient.post()
            .uri("/api/teams")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "name": "Local team",
                  "administratorReference": "USR-2026-1001-00000ZA7B901",
                  "managerReference": "USR-2026-1001-00000ZA7B902"
                }
                """)
            .exchange()
            .returnResult(ApiError.class);
        ApiError error = result.getResponseBody();

        assertEquals(409, result.getStatus().value());
        assertEquals("TEAM_ORGANIZATION_UNAVAILABLE", error.code());
        assertEquals(
            OrganizationLifecycleState.CREATING,
            organizationResponsibilities.findByOrganizationReference(
                tenantContext.tenantReference()
            ).orElseThrow().status()
        );
    }
}
