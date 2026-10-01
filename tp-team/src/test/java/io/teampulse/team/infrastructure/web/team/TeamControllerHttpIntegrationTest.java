package io.teampulse.team.infrastructure.web.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.error.ApiError;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.team.AbstractTeamHttpIntegrationTest;
import io.teampulse.team.TeamTestApplication.DeterministicTenantContextProvider;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamControllerHttpIntegrationTest extends AbstractTeamHttpIntegrationTest {

    private static final String TENANT_REFERENCE = "ORG-2026-0916-00000ZA7B950";
    private static final String OTHER_TENANT_REFERENCE = "ORG-2026-0916-00000ZA7B951";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-0916-00000ZA7B950";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B951";
    private static final String MEMBER_REFERENCE = "USR-2026-0916-00000ZA7B952";
    private static final String REPLACEMENT_REFERENCE = "USR-2026-0916-00000ZA7B953";
    private static final String OTHER_MEMBER_REFERENCE = "USR-2026-0916-00000ZA7B954";

    @Value("${local.server.port}")
    private int port;

    @Inject
    private DeterministicTenantContextProvider tenantContextProvider;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @MockitoBean
    private OrganizationDirectory organizationDirectory;

    @MockitoBean
    private UserDirectory userDirectory;

    @BeforeEach
    void resetExternalContractsAndTenantProvider() {
        tenantContextProvider.setTenantContext(new TenantContext(TENANT_REFERENCE));
        tenantContextProvider.resetCalls();
        when(organizationDirectory.check(anyString()))
            .thenReturn(OrganizationAvailability.AVAILABLE);
        when(userDirectory.check(anyString(), anyString()))
            .thenReturn(UserAvailability.AVAILABLE);
    }

    @Test
    void createsAndPersistsAnActiveTeamInTheProviderTenant() {
        var response = api().createTeam(
            "Platform Engineering",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE
        );
        var team = response.getBody();

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(team);
        assertEquals("ACTIVE", team.getStatus().getValue());
        assertEquals(1, tenantContextProvider.calls());

        TeamEntity persisted = persistedTeam(team.getReference());
        assertEquals(TENANT_REFERENCE, persisted.getOrganizationReference());
        assertEquals(TeamStatus.ACTIVE, persisted.getStatus());
        assertEquals(ADMINISTRATOR_REFERENCE, persisted.getAdminReference());
        assertEquals(MANAGER_REFERENCE, persisted.getManagerReference());
        assertFalse(response.getBody().toString().contains("organizationReference"));
    }

    @Test
    void ignoresAClientSuppliedOrganizationReferenceAndUsesTheProviderTenant() {
        var team = restTestClient.post()
            .uri("/api/teams")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "name": "Tenant boundary",
                  "administratorReference": "USR-2026-0916-00000ZA7B950",
                  "managerReference": "USR-2026-0916-00000ZA7B951",
                  "organizationReference": "ORG-2026-0916-00000ZA7B951"
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(org.openapitools.client.model.TeamResponse.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(team);
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TENANT_REFERENCE, persistedTeam(team.getReference()).getOrganizationReference());
        assertFalse(jpaTeamRepository.findByOrganizationReferenceAndReference(
            OTHER_TENANT_REFERENCE,
            team.getReference()
        ).isPresent());
    }

    @Test
    void refusesCreationWhenTheProviderOrganizationIsNotAvailable() {
        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.UNAVAILABLE);

        ApiError error = restTestClient.post()
            .uri("/api/teams")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "name": "Blocked team",
                  "administratorReference": "USR-2026-0916-00000ZA7B950",
                  "managerReference": "USR-2026-0916-00000ZA7B951"
                }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("TEAM_ORGANIZATION_UNAVAILABLE", error.code());
        assertEquals("Team organization is not available", error.message());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(0L, jpaTeamRepository.count());
        verify(userDirectory, never()).check(anyString(), anyString());
    }

    @Test
    void returnsASanitizedSharedRequestErrorBeforeResolvingTheTenant() {
        ApiError error = restTestClient.post()
            .uri("/api/teams")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "name": " ",
                  "administratorReference": "USR-2026-0916-00000ZA7B950",
                  "managerReference": "USR-2026-0916-00000ZA7B951"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("VALIDATION_FAILED", error.code());
        assertEquals("Request validation failed", error.message());
        assertEquals(0, tenantContextProvider.calls());
        assertEquals(0L, jpaTeamRepository.count());
    }

    @Test
    void mapsAnInvalidTeamNameToASanitizedLocalBusinessError() {
        ApiError error = restTestClient.post()
            .uri("/api/teams")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "name": "%s",
                  "administratorReference": "USR-2026-0916-00000ZA7B950",
                  "managerReference": "USR-2026-0916-00000ZA7B951"
                }
                """.formatted("x".repeat(201)))
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("INVALID_TEAM_NAME", error.code());
        assertEquals("Team name is invalid", error.message());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(0L, jpaTeamRepository.count());
    }

    @Test
    void cannotMutateATeamFromAnotherProviderTenant() {
        var created = api().createTeam(
            "Tenant-owned team",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE
        ).getBody();
        assertNotNull(created);

        tenantContextProvider.setTenantContext(new TenantContext(OTHER_TENANT_REFERENCE));
        tenantContextProvider.resetCalls();
        ApiError error = restTestClient.post()
            .uri("/api/teams/{teamReference}/suspension", created.getReference())
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("TEAM_NOT_FOUND", error.code());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TeamStatus.ACTIVE, persistedTeam(created.getReference()).getStatus());
    }

    @Test
    void exposesTeamLifecycleAndRejectsReactivationForAnUnavailableOrganization() {
        var created = api().createTeam(
            "Lifecycle team",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE
        ).getBody();
        assertNotNull(created);

        tenantContextProvider.resetCalls();
        var suspended = api().suspendTeam(created.getReference());
        assertEquals(200, suspended.getStatusCode().value());
        assertEquals("SUSPENDED", suspended.getBody().getStatus().getValue());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TeamStatus.SUSPENDED, persistedTeam(created.getReference()).getStatus());

        tenantContextProvider.resetCalls();
        ApiError teamUnavailable = restTestClient.post()
            .uri("/api/teams/{teamReference}/invitations", created.getReference())
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "userReference": "USR-2026-0916-00000ZA7B954" }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(teamUnavailable);
        assertEquals("TEAM_UNAVAILABLE", teamUnavailable.code());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(0L, jpaTeamMemberRepository.count());

        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.UNAVAILABLE);
        tenantContextProvider.resetCalls();
        ApiError unavailable = restTestClient.post()
            .uri("/api/teams/{teamReference}/reactivation", created.getReference())
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(unavailable);
        assertEquals("TEAM_ORGANIZATION_UNAVAILABLE", unavailable.code());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TeamStatus.SUSPENDED, persistedTeam(created.getReference()).getStatus());

        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.AVAILABLE);
        tenantContextProvider.resetCalls();
        var reactivated = api().reactivateTeam(created.getReference());
        assertEquals(200, reactivated.getStatusCode().value());
        assertEquals("ACTIVE", reactivated.getBody().getStatus().getValue());
        assertEquals(1, tenantContextProvider.calls());

        tenantContextProvider.resetCalls();
        var archived = api().archiveTeam(created.getReference());
        assertEquals(200, archived.getStatusCode().value());
        assertEquals("ARCHIVED", archived.getBody().getStatus().getValue());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TeamStatus.ARCHIVED, persistedTeam(created.getReference()).getStatus());
    }

    @Test
    void replacesOnlyAvailableResponsibleUsersWithoutChangingTeamStatus() {
        var created = api().createTeam(
            "Responsible team",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE
        ).getBody();
        assertNotNull(created);

        when(userDirectory.check(TENANT_REFERENCE, REPLACEMENT_REFERENCE))
            .thenReturn(UserAvailability.UNAVAILABLE);
        tenantContextProvider.resetCalls();
        ApiError error = restTestClient.put()
            .uri("/api/teams/{teamReference}/administrator", created.getReference())
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "userReference": "USR-2026-0916-00000ZA7B953" }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("TEAM_ADMINISTRATOR_NOT_AVAILABLE", error.code());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(ADMINISTRATOR_REFERENCE, persistedTeam(created.getReference()).getAdminReference());
        assertEquals(TeamStatus.ACTIVE, persistedTeam(created.getReference()).getStatus());

        when(userDirectory.check(TENANT_REFERENCE, REPLACEMENT_REFERENCE))
            .thenReturn(UserAvailability.AVAILABLE);
        tenantContextProvider.resetCalls();
        var replaced = api().replaceAdministrator(created.getReference(), REPLACEMENT_REFERENCE);
        assertEquals(200, replaced.getStatusCode().value());
        assertEquals(REPLACEMENT_REFERENCE, replaced.getBody().getAdministratorReference());
        assertEquals(TeamStatus.ACTIVE, persistedTeam(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());

        when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE))
            .thenReturn(UserAvailability.UNAVAILABLE);
        tenantContextProvider.resetCalls();
        ApiError unavailableManager = restTestClient.put()
            .uri("/api/teams/{teamReference}/manager", created.getReference())
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "userReference": "USR-2026-0916-00000ZA7B952" }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(unavailableManager);
        assertEquals("TEAM_MANAGER_NOT_AVAILABLE", unavailableManager.code());
        assertEquals(MANAGER_REFERENCE, persistedTeam(created.getReference()).getManagerReference());
        assertEquals(TeamStatus.ACTIVE, persistedTeam(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());

        when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE))
            .thenReturn(UserAvailability.AVAILABLE);
        tenantContextProvider.resetCalls();
        var replacedManager = api().replaceManager(created.getReference(), MEMBER_REFERENCE);
        assertEquals(200, replacedManager.getStatusCode().value());
        assertEquals(MEMBER_REFERENCE, replacedManager.getBody().getManagerReference());
        assertEquals(TeamStatus.ACTIVE, persistedTeam(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void keepsMemberInvitationSeparateFromIdentityActivationAndEnforcesAvailability() {
        var created = api().createTeam(
            "Membership team",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE
        ).getBody();
        assertNotNull(created);
        when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE))
            .thenReturn(UserAvailability.PENDING);

        tenantContextProvider.resetCalls();
        var invited = api().inviteMember(created.getReference(), MEMBER_REFERENCE);
        assertEquals(201, invited.getStatusCode().value());
        assertEquals("INVITED", invited.getBody().getStatus().getValue());
        assertNull(invited.getBody().getStartedAt());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TeamMemberStatus.INVITED, persistedMember(created.getReference()).getStatus());

        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.UNAVAILABLE);
        tenantContextProvider.resetCalls();
        ApiError organizationError = restTestClient.post()
            .uri("/api/teams/{teamReference}/members/{userReference}/activation", created.getReference(), MEMBER_REFERENCE)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(organizationError);
        assertEquals("TEAM_ORGANIZATION_UNAVAILABLE", organizationError.code());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TeamMemberStatus.INVITED, persistedMember(created.getReference()).getStatus());

        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.AVAILABLE);
        tenantContextProvider.resetCalls();
        ApiError pendingUserError = restTestClient.post()
            .uri("/api/teams/{teamReference}/members/{userReference}/activation", created.getReference(), MEMBER_REFERENCE)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(pendingUserError);
        assertEquals("TEAM_MEMBER_USER_NOT_AVAILABLE", pendingUserError.code());
        assertEquals(1, tenantContextProvider.calls());
        assertEquals(TeamMemberStatus.INVITED, persistedMember(created.getReference()).getStatus());

        when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE))
            .thenReturn(UserAvailability.AVAILABLE);
        tenantContextProvider.resetCalls();
        var activated = api().activateMember(created.getReference(), MEMBER_REFERENCE);
        assertEquals(200, activated.getStatusCode().value());
        assertEquals("ACTIVE", activated.getBody().getStatus().getValue());
        assertNotNull(activated.getBody().getStartedAt());
        assertEquals(TeamMemberStatus.ACTIVE, persistedMember(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());

        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.UNAVAILABLE);
        when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE))
            .thenReturn(UserAvailability.UNAVAILABLE);
        tenantContextProvider.resetCalls();
        var suspended = api().suspendMember(created.getReference(), MEMBER_REFERENCE);
        assertEquals(200, suspended.getStatusCode().value());
        assertEquals("SUSPENDED", suspended.getBody().getStatus().getValue());
        assertEquals(TeamMemberStatus.SUSPENDED, persistedMember(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());

        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.AVAILABLE);
        when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE))
            .thenReturn(UserAvailability.AVAILABLE);
        tenantContextProvider.resetCalls();
        var reactivated = api().reactivateMember(created.getReference(), MEMBER_REFERENCE);
        assertEquals(200, reactivated.getStatusCode().value());
        assertEquals("ACTIVE", reactivated.getBody().getStatus().getValue());
        assertEquals(TeamMemberStatus.ACTIVE, persistedMember(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());

        when(organizationDirectory.check(TENANT_REFERENCE))
            .thenReturn(OrganizationAvailability.UNAVAILABLE);
        tenantContextProvider.resetCalls();
        var suspendedTeam = api().suspendTeam(created.getReference());
        assertEquals(200, suspendedTeam.getStatusCode().value());
        assertEquals(TeamStatus.SUSPENDED, persistedTeam(created.getReference()).getStatus());

        tenantContextProvider.resetCalls();
        var suspendedOnUnavailableTeam = api().suspendMember(created.getReference(), MEMBER_REFERENCE);
        assertEquals(200, suspendedOnUnavailableTeam.getStatusCode().value());
        assertEquals("SUSPENDED", suspendedOnUnavailableTeam.getBody().getStatus().getValue());
        assertEquals(TeamMemberStatus.SUSPENDED, persistedMember(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());

        tenantContextProvider.resetCalls();
        var removed = api().removeMember(created.getReference(), MEMBER_REFERENCE);
        assertEquals(200, removed.getStatusCode().value());
        assertEquals("REMOVED", removed.getBody().getStatus().getValue());
        assertNotNull(removed.getBody().getEndedAt());
        assertEquals(TeamMemberStatus.REMOVED, persistedMember(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());
    }

    @Test
    void addsAnAvailableUserAsAnActiveMember() {
        var created = api().createTeam(
            "Active membership team",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE
        ).getBody();
        assertNotNull(created);

        tenantContextProvider.resetCalls();
        var added = api().addMember(created.getReference(), MEMBER_REFERENCE);
        assertEquals(201, added.getStatusCode().value());
        assertEquals("ACTIVE", added.getBody().getStatus().getValue());
        assertNotNull(added.getBody().getStartedAt());
        assertEquals(TeamMemberStatus.ACTIVE, persistedMember(created.getReference()).getStatus());
        assertEquals(1, tenantContextProvider.calls());
    }

    private TeamHttpDsl api() {
        return TeamHttpDsl.runningAt(port);
    }

    private TeamEntity persistedTeam(String teamReference) {
        return jpaTeamRepository.findByOrganizationReferenceAndReference(
            TENANT_REFERENCE,
            teamReference
        ).orElseThrow();
    }

    private TeamMemberEntity persistedMember(String teamReference) {
        TeamEntity team = persistedTeam(teamReference);
        return jpaTeamMemberRepository
            .findByOrganizationReferenceAndTeamIdAndUserReferenceAndStatusIn(
                TENANT_REFERENCE,
                team.getId(),
                MEMBER_REFERENCE,
                EnumSet.allOf(TeamMemberStatus.class)
            )
            .orElseThrow();
    }
}
