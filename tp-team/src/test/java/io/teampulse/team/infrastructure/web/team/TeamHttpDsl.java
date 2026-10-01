package io.teampulse.team.infrastructure.web.team;

import org.openapitools.client.ApiClient;
import org.openapitools.client.api.TeamsApi;
import org.openapitools.client.model.CreateTeamInput;
import org.openapitools.client.model.ResponsibleUserInput;
import org.openapitools.client.model.TeamMemberInput;
import org.openapitools.client.model.TeamMemberResponse;
import org.openapitools.client.model.TeamResponse;
import org.springframework.http.ResponseEntity;

/** Module-local DSL backed by the client generated from team's OpenAPI contract. */
final class TeamHttpDsl {

    private final TeamsApi teamsApi;

    private TeamHttpDsl(TeamsApi teamsApi) {
        this.teamsApi = teamsApi;
    }

    static TeamHttpDsl runningAt(int port) {
        ApiClient apiClient = new ApiClient()
            .setBasePath("http://localhost:" + port);
        return new TeamHttpDsl(new TeamsApi(apiClient));
    }

    ResponseEntity<TeamResponse> createTeam(
        String name,
        String administratorReference,
        String managerReference
    ) {
        return teamsApi.createTeamWithHttpInfo(new CreateTeamInput()
            .name(name)
            .administratorReference(administratorReference)
            .managerReference(managerReference));
    }

    ResponseEntity<TeamResponse> suspendTeam(String teamReference) {
        return teamsApi.suspendTeamWithHttpInfo(teamReference);
    }

    ResponseEntity<TeamResponse> reactivateTeam(String teamReference) {
        return teamsApi.reactivateTeamWithHttpInfo(teamReference);
    }

    ResponseEntity<TeamResponse> archiveTeam(String teamReference) {
        return teamsApi.archiveTeamWithHttpInfo(teamReference);
    }

    ResponseEntity<TeamResponse> replaceAdministrator(
        String teamReference,
        String userReference
    ) {
        return teamsApi.replaceTeamAdministratorWithHttpInfo(
            teamReference,
            responsibleUser(userReference)
        );
    }

    ResponseEntity<TeamResponse> replaceManager(
        String teamReference,
        String userReference
    ) {
        return teamsApi.replaceTeamManagerWithHttpInfo(
            teamReference,
            responsibleUser(userReference)
        );
    }

    ResponseEntity<TeamMemberResponse> addMember(String teamReference, String userReference) {
        return teamsApi.addTeamMemberWithHttpInfo(teamReference, member(userReference));
    }

    ResponseEntity<TeamMemberResponse> inviteMember(String teamReference, String userReference) {
        return teamsApi.inviteTeamMemberWithHttpInfo(teamReference, member(userReference));
    }

    ResponseEntity<TeamMemberResponse> activateMember(String teamReference, String userReference) {
        return teamsApi.activateTeamMemberWithHttpInfo(teamReference, userReference);
    }

    ResponseEntity<TeamMemberResponse> suspendMember(String teamReference, String userReference) {
        return teamsApi.suspendTeamMemberWithHttpInfo(teamReference, userReference);
    }

    ResponseEntity<TeamMemberResponse> reactivateMember(String teamReference, String userReference) {
        return teamsApi.reactivateTeamMemberWithHttpInfo(teamReference, userReference);
    }

    ResponseEntity<TeamMemberResponse> removeMember(String teamReference, String userReference) {
        return teamsApi.removeTeamMemberWithHttpInfo(teamReference, userReference);
    }

    private static ResponsibleUserInput responsibleUser(String userReference) {
        return new ResponsibleUserInput().userReference(userReference);
    }

    private static TeamMemberInput member(String userReference) {
        return new TeamMemberInput().userReference(userReference);
    }
}
