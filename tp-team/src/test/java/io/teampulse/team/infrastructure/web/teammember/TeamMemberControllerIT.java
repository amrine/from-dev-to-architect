package io.teampulse.team.infrastructure.web.teammember;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.team.AbstractTeamMvcIT;
import io.teampulse.team.application.port.out.team.TeamMemberRepository;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.testsupport.context.DeterministicTenantContextProvider;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamMemberControllerIT extends AbstractTeamMvcIT {

    private static final String TENANT_REFERENCE = "ORG-2026-0916-00000ZA7B950";
    private static final String OTHER_TENANT_REFERENCE = "ORG-2026-0916-00000ZA7B951";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B950";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-0916-00000ZA7B950";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B951";
    private static final String MEMBER_REFERENCE = "USR-2026-0916-00000ZA7B952";
    private static final Instant STARTED_AT = Instant.parse("2026-09-01T09:00:00Z");
    private static final Instant CLOCK_NOW = Instant.parse("2026-09-16T08:30:00Z");

    @Inject
    private DeterministicTenantContextProvider tenantContextProvider;

    @Inject
    private TeamRepository teamRepository;

    @Inject
    private TeamMemberRepository teamMemberRepository;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

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
    class AddMember {

        @Test
        void addsAndPersistsAnActiveMember() throws Exception {
            Team team = persistTeam();

            MvcResult result = mockMvc.perform(post("/api/teams/{teamReference}/members", TEAM_REFERENCE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TeamMemberRequest(MEMBER_REFERENCE))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userReference").value(MEMBER_REFERENCE))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.startedAt").value(CLOCK_NOW.toString()))
                    .andExpect(jsonPath("$.endedAt").doesNotExist())
                    .andReturn();

            TeamMemberResponse response =
                    objectMapper.readValue(result.getResponse().getContentAsString(), TeamMemberResponse.class);
            TeamMemberEntity persistedMember = persistedMember(team, MEMBER_REFERENCE);
            assertEquals(CLOCK_NOW, response.startedAt());
            assertEquals(TeamMemberStatus.ACTIVE, persistedMember.getStatus());
            assertEquals(CLOCK_NOW, persistedMember.getStartedAt());
            assertNull(persistedMember.getEndedAt());
            assertEquals(1L, jpaTeamMemberRepository.count());
            assertTenantResolvedOnce();
        }

        @Test
        void rejectsAnUnavailableUserWithoutCreatingAMembership() throws Exception {
            persistTeam();
            when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE)).thenReturn(UserAvailability.UNAVAILABLE);

            mockMvc.perform(post("/api/teams/{teamReference}/members", TEAM_REFERENCE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TeamMemberRequest(MEMBER_REFERENCE))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TEAM_MEMBER_USER_NOT_AVAILABLE"))
                    .andExpect(jsonPath("$.message").value("Team member user is not available"));

            assertEquals(0L, jpaTeamMemberRepository.count());
            assertTenantResolvedOnce();
        }

        @Test
        void rejectsARequestWithNoUserReferenceBeforeResolvingTheTenant() throws Exception {
            persistTeam();

            mockMvc.perform(post("/api/teams/{teamReference}/members", TEAM_REFERENCE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TeamMemberRequest(" "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            assertEquals(0L, jpaTeamMemberRepository.count());
            assertEquals(0, tenantContextProvider.calls());
        }

        @Test
        void cannotAddAMemberToATeamOwnedByAnotherTenant() throws Exception {
            persistTeam();
            tenantContextProvider.setTenantContext(new TenantContext(OTHER_TENANT_REFERENCE));

            mockMvc.perform(post("/api/teams/{teamReference}/members", TEAM_REFERENCE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TeamMemberRequest(MEMBER_REFERENCE))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

            assertEquals(0L, jpaTeamMemberRepository.count());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class InviteMember {

        @Test
        void createsAnInvitationForAPendingUserWithoutStartingMembershipHistory() throws Exception {
            Team team = persistTeam();
            when(userDirectory.check(TENANT_REFERENCE, MEMBER_REFERENCE)).thenReturn(UserAvailability.PENDING);

            MvcResult result = mockMvc.perform(post("/api/teams/{teamReference}/invitations", TEAM_REFERENCE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TeamMemberRequest(MEMBER_REFERENCE))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("INVITED"))
                    .andExpect(jsonPath("$.startedAt").doesNotExist())
                    .andExpect(jsonPath("$.endedAt").doesNotExist())
                    .andReturn();

            TeamMemberResponse response =
                    objectMapper.readValue(result.getResponse().getContentAsString(), TeamMemberResponse.class);
            TeamMemberEntity persistedMember = persistedMember(team, MEMBER_REFERENCE);
            assertNull(response.startedAt());
            assertNull(response.endedAt());
            assertEquals(TeamMemberStatus.INVITED, persistedMember.getStatus());
            assertNull(persistedMember.getStartedAt());
            assertNull(persistedMember.getEndedAt());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class ActivateMember {

        @Test
        void activatesAnInvitationAndPersistsItsStartTime() throws Exception {
            Team team = persistTeam();
            persistMember(team, TeamMemberStatus.INVITED);

            MvcResult result = mockMvc.perform(post(
                            "/api/teams/{teamReference}/members/{userReference}/activation",
                            TEAM_REFERENCE,
                            MEMBER_REFERENCE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.startedAt").value(CLOCK_NOW.toString()))
                    .andReturn();

            TeamMemberResponse response =
                    objectMapper.readValue(result.getResponse().getContentAsString(), TeamMemberResponse.class);
            TeamMemberEntity persistedMember = persistedMember(team, MEMBER_REFERENCE);
            assertEquals(CLOCK_NOW, response.startedAt());
            assertEquals(TeamMemberStatus.ACTIVE, persistedMember.getStatus());
            assertEquals(CLOCK_NOW, persistedMember.getStartedAt());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class SuspendMember {

        @Test
        void suspendsAnActiveMembershipWithoutEndingItsHistory() throws Exception {
            Team team = persistTeam();
            persistMember(team, TeamMemberStatus.ACTIVE);

            mockMvc.perform(post(
                            "/api/teams/{teamReference}/members/{userReference}/suspension",
                            TEAM_REFERENCE,
                            MEMBER_REFERENCE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));

            TeamMemberEntity persistedMember = persistedMember(team, MEMBER_REFERENCE);
            assertEquals(TeamMemberStatus.SUSPENDED, persistedMember.getStatus());
            assertEquals(STARTED_AT, persistedMember.getStartedAt());
            assertNull(persistedMember.getEndedAt());
            assertTenantResolvedOnce();
        }

        @Test
        void mapsAnInvalidMembershipTransitionWithoutChangingItsStatus() throws Exception {
            Team team = persistTeam();
            persistMember(team, TeamMemberStatus.INVITED);

            mockMvc.perform(post(
                            "/api/teams/{teamReference}/members/{userReference}/suspension",
                            TEAM_REFERENCE,
                            MEMBER_REFERENCE))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TEAM_MEMBER_TRANSITION_NOT_ALLOWED"));

            assertEquals(
                    TeamMemberStatus.INVITED,
                    persistedMember(team, MEMBER_REFERENCE).getStatus());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class ReactivateMember {

        @Test
        void reactivatesASuspendedMembershipAndKeepsItsOriginalStartTime() throws Exception {
            Team team = persistTeam();
            persistMember(team, TeamMemberStatus.SUSPENDED);

            mockMvc.perform(post(
                            "/api/teams/{teamReference}/members/{userReference}/reactivation",
                            TEAM_REFERENCE,
                            MEMBER_REFERENCE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            TeamMemberEntity persistedMember = persistedMember(team, MEMBER_REFERENCE);
            assertEquals(TeamMemberStatus.ACTIVE, persistedMember.getStatus());
            assertEquals(STARTED_AT, persistedMember.getStartedAt());
            assertNull(persistedMember.getEndedAt());
            assertTenantResolvedOnce();
        }
    }

    @Nested
    class RemoveMember {

        @Test
        void endsTheMembershipAndRetainsItsHistory() throws Exception {
            Team team = persistTeam();
            persistMember(team, TeamMemberStatus.ACTIVE);

            MvcResult result = mockMvc.perform(post(
                            "/api/teams/{teamReference}/members/{userReference}/removal",
                            TEAM_REFERENCE,
                            MEMBER_REFERENCE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REMOVED"))
                    .andExpect(jsonPath("$.endedAt").value(CLOCK_NOW.toString()))
                    .andReturn();

            TeamMemberResponse response =
                    objectMapper.readValue(result.getResponse().getContentAsString(), TeamMemberResponse.class);
            TeamMemberEntity persistedMember = persistedMember(team, MEMBER_REFERENCE);
            assertEquals(CLOCK_NOW, response.endedAt());
            assertEquals(TeamMemberStatus.REMOVED, persistedMember.getStatus());
            assertEquals(STARTED_AT, persistedMember.getStartedAt());
            assertEquals(CLOCK_NOW, persistedMember.getEndedAt());
            assertEquals(1L, jpaTeamMemberRepository.count());
            assertTenantResolvedOnce();
        }

        @Test
        void returnsNotFoundWhenTheMembershipDoesNotExist() throws Exception {
            persistTeam();

            mockMvc.perform(post(
                            "/api/teams/{teamReference}/members/{userReference}/removal",
                            TEAM_REFERENCE,
                            MEMBER_REFERENCE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TEAM_MEMBER_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Team member was not found"));

            assertEquals(0L, jpaTeamMemberRepository.count());
            assertTenantResolvedOnce();
        }
    }

    private Team persistTeam() {
        return persistTeam(TeamStatus.ACTIVE);
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

    private void persistMember(Team team, TeamMemberStatus status) {
        TeamMember member = switch (status) {
            case INVITED -> TeamMember.invite(TENANT_REFERENCE, team.getId(), MEMBER_REFERENCE);
            case ACTIVE -> TeamMember.add(TENANT_REFERENCE, team.getId(), MEMBER_REFERENCE, STARTED_AT);
            case SUSPENDED -> {
                TeamMember activeMember = TeamMember.add(TENANT_REFERENCE, team.getId(), MEMBER_REFERENCE, STARTED_AT);
                activeMember.suspend();
                yield activeMember;
            }
            case REMOVED -> {
                TeamMember activeMember = TeamMember.add(TENANT_REFERENCE, team.getId(), MEMBER_REFERENCE, STARTED_AT);
                activeMember.remove(CLOCK_NOW);
                yield activeMember;
            }
        };
        teamMemberRepository.create(member);
    }

    private TeamMemberEntity persistedMember(Team team, String userReference) {
        return jpaTeamMemberRepository
                .findByOrganizationReferenceAndTeamIdAndUserReferenceAndStatusIn(
                        TENANT_REFERENCE, team.getId(), userReference, EnumSet.allOf(TeamMemberStatus.class))
                .orElseThrow();
    }

    private void assertTenantResolvedOnce() {
        assertEquals(1, tenantContextProvider.calls());
    }
}
