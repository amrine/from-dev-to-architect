package io.teampulse.team.infrastructure.web.team;

import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamMapperTest {

    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B950";
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B950";
    private static final String ADMINISTRATOR_REFERENCE = "USR-2026-0916-00000ZA7B950";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B951";

    private final TeamMapper mapper = Mappers.getMapper(TeamMapper.class);

    @Test
    void mapsCreateRequestToCommand() {
        CreateTeamRequest request = new CreateTeamRequest("Platform", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);

        assertEquals(
                new CreateTeamCommand("Platform", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE),
                mapper.toCommand(request));
    }

    @Test
    void combinesTeamReferenceAndResponsibleUserRequest() {
        TeamResponsibleUserRequest request = new TeamResponsibleUserRequest(MANAGER_REFERENCE);

        assertEquals(
                new TeamResponsibleCommand(TEAM_REFERENCE, MANAGER_REFERENCE),
                mapper.toResponsibleCommand(TEAM_REFERENCE, request));
    }

    @Test
    void mapsDomainTeamToResponse() {
        Team team = Team.create(
                TEAM_REFERENCE, ORGANIZATION_REFERENCE, "Platform", ADMINISTRATOR_REFERENCE, MANAGER_REFERENCE);

        assertEquals(
                new TeamResponse(
                        TEAM_REFERENCE,
                        "Platform",
                        TeamStatus.ACTIVE.name(),
                        ADMINISTRATOR_REFERENCE,
                        MANAGER_REFERENCE),
                mapper.toResponse(team));
    }
}
