package io.teampulse.team.infrastructure.web.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.team.AbstractTeamMvcIT;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.testsupport.context.DeterministicTenantContextProvider;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamControllerIT extends AbstractTeamMvcIT {

    private static final String TENANT_REFERENCE = "ORG-2026-0916-00000ZA7B950";
    private static final String OTHER_TENANT_REFERENCE = "ORG-2026-0916-00000ZA7B951";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B950";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-0916-00000ZA7B950";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B951";
    private static final String REPLACEMENT_REFERENCE = "USR-2026-0916-00000ZA7B953";

    @Inject
    private DeterministicTenantContextProvider tenantContextProvider;

    @Inject
    private TeamRepository teamRepository;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @MockitoBean
    private OrganizationDirectory organizationDirectory;

    @MockitoBean
    private UserDirectory userDirectory;

    @BeforeEach
    void configureExternalContractsAndTenant() {
        tenantContextProvider.setTenantContext(new TenantContext(TENANT_REFERENCE));
        tenantContextProvider.resetCalls();
        when(organizationDirectory.check(anyString())).thenReturn(OrganizationAvailability.AVAILABLE);
        when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.AVAILABLE);
    }

    @Nested
    class CreateTeam {

        @Test
        void createsAndPersistsAnActiveTeamInTheResolvedTenant() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateTeamRequest(
                                    " TeamPulse Engineering ", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("TeamPulse Engineering"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.administratorReference").value(ADMINISTRATOR_REFERENCE))
                    .andExpect(jsonPath("$.managerReference").value(MANAGER_REFERENCE))
                    .andReturn();

            TeamResponse response =
                    objectMapper.readValue(result.getResponse().getContentAsString(), TeamResponse.class);
            TeamEntity persisted = persistedTeam(TENANT_REFERENCE, response.reference());
            assertEquals(TENANT_REFERENCE, persisted.getOrganizationReference());
            assertEquals("TeamPulse Engineering", persisted.getName());
            assertEquals(TeamStatus.ACTIVE, persisted.getStatus());
            assertEquals(1L, jpaTeamRepository.count());
            assertTenantResolvedOnce();
        }

        @Test
        void rejectsABlankRequiredFieldBeforeResolvingTheTenant() throws Exception {
            mockMvc.perform(post("/api/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateTeamRequest(" ", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("Request validation failed"));

            assertEquals(0L, jpaTeamRepository.count());
            assertEquals(0, tenantContextProvider.calls());
        }
    }

    @Nested
    class SuspendTeam {

        @Test
        void suspendsAndPersistsAnActiveTeam() throws Exception {
            persistTeam(TeamStatus.ACTIVE);

            mockMvc.perform(post("/api/teams/{teamReference}/suspension", TEAM_REFERENCE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));

            assertEquals(TeamStatus.SUSPENDED, persistedTeam().getStatus());
            assertTenantResolvedOnce();
        }

        @Test
        void returnsNotFoundWhenTheTeamBelongsToAnotherTenant() throws Exception {
            persistTeam(TeamStatus.ACTIVE);
            tenantContextProvider.setTenantContext(new TenantContext(OTHER_TENANT_REFERENCE));

            mockMvc.perform(post("/api/teams/{teamReference}/suspension", TEAM_REFERENCE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Team was not found"));

            assertEquals(TeamStatus.ACTIVE, persistedTeam().getStatus());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class ReactivateTeam {

        @Test
        void reactivatesAndPersistsASuspendedTeam() throws Exception {
            persistTeam(TeamStatus.SUSPENDED);

            mockMvc.perform(post("/api/teams/{teamReference}/reactivation", TEAM_REFERENCE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            assertEquals(TeamStatus.ACTIVE, persistedTeam().getStatus());
            assertTenantResolvedOnce();
        }

        @Test
        void mapsUnavailableResponsibleUserToAConflictWithoutMutation() throws Exception {
            persistTeam(TeamStatus.SUSPENDED);
            when(userDirectory.check(TENANT_REFERENCE, ADMINISTRATOR_REFERENCE))
                    .thenReturn(UserAvailability.UNAVAILABLE);

            mockMvc.perform(post("/api/teams/{teamReference}/reactivation", TEAM_REFERENCE))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TEAM_ADMINISTRATOR_NOT_AVAILABLE"))
                    .andExpect(jsonPath("$.message").value("Team administrator is not available"));

            assertEquals(TeamStatus.SUSPENDED, persistedTeam().getStatus());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class ArchiveTeam {

        @Test
        void archivesAndPersistsAnActiveTeam() throws Exception {
            persistTeam(TeamStatus.ACTIVE);

            mockMvc.perform(post("/api/teams/{teamReference}/archive", TEAM_REFERENCE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));

            assertEquals(TeamStatus.ARCHIVED, persistedTeam().getStatus());
            assertTenantResolvedOnce();
        }

        @Test
        void rejectsArchivingAnAlreadyArchivedTeamWithoutMutation() throws Exception {
            persistTeam(TeamStatus.ARCHIVED);

            mockMvc.perform(post("/api/teams/{teamReference}/archive", TEAM_REFERENCE))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TEAM_TRANSITION_NOT_ALLOWED"));

            assertEquals(TeamStatus.ARCHIVED, persistedTeam().getStatus());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class ReplaceResponsibleUsers {

        @ParameterizedTest
        @CsvSource({"administrator", "manager"})
        void replacesAndPersistsEachResponsibleUser(String role) throws Exception {
            persistTeam(TeamStatus.ACTIVE);

            mockMvc.perform(put("/api/teams/{teamReference}/{role}", TEAM_REFERENCE, role)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new TeamResponsibleUserRequest(REPLACEMENT_REFERENCE))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + role + "Reference").value(REPLACEMENT_REFERENCE));

            TeamEntity persisted = persistedTeam();
            assertEquals(
                    REPLACEMENT_REFERENCE,
                    role.equals("administrator") ? persisted.getAdminReference() : persisted.getManagerReference());
            assertTenantResolvedOnce();
        }

        @ParameterizedTest
        @CsvSource({"administrator,TEAM_ADMINISTRATOR_NOT_AVAILABLE", "manager,TEAM_MANAGER_NOT_AVAILABLE"})
        void rejectsAnUnavailableReplacementWithoutChangingTheTeam(String role, String errorCode) throws Exception {
            persistTeam(TeamStatus.ACTIVE);
            when(userDirectory.check(TENANT_REFERENCE, REPLACEMENT_REFERENCE)).thenReturn(UserAvailability.UNAVAILABLE);

            mockMvc.perform(put("/api/teams/{teamReference}/{role}", TEAM_REFERENCE, role)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new TeamResponsibleUserRequest(REPLACEMENT_REFERENCE))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(errorCode));

            TeamEntity persisted = persistedTeam();
            assertEquals(ADMINISTRATOR_REFERENCE, persisted.getAdminReference());
            assertEquals(MANAGER_REFERENCE, persisted.getManagerReference());
            assertTenantResolvedOnce();
        }
    }

    private Team persistTeam(TeamStatus status) {
        Team team = Team.create(
                TEAM_REFERENCE, TENANT_REFERENCE, "TeamPulse Engineering", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);
        switch (status) {
            case ACTIVE -> {}
            case SUSPENDED -> team.suspend();
            case ARCHIVED -> team.archive();
        }
        return teamRepository.create(team);
    }

    private TeamEntity persistedTeam() {
        return persistedTeam(TENANT_REFERENCE, TEAM_REFERENCE);
    }

    private TeamEntity persistedTeam(String tenantReference, String teamReference) {
        return jpaTeamRepository
                .findByOrganizationReferenceAndReference(tenantReference, teamReference)
                .orElseThrow();
    }

    private void assertTenantResolvedOnce() {
        assertEquals(1, tenantContextProvider.calls());
    }
}
