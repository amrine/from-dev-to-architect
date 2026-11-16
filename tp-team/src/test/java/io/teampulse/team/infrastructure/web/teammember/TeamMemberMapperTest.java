package io.teampulse.team.infrastructure.web.teammember;

import io.teampulse.team.application.port.in.teammember.TeamMemberCommand;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamMemberMapperTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B950";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B950";
    private static final String USER_REFERENCE = "USR-2026-0916-00000ZA7B950";
    private static final Instant STARTED_AT = Instant.parse("2026-09-16T10:15:30Z");
    private static final Instant ENDED_AT = Instant.parse("2026-09-16T11:15:30Z");

    private final TeamMemberMapper mapper = Mappers.getMapper(TeamMemberMapper.class);

    @Test
    void combinesTeamReferenceAndMemberRequest() {
        TeamMemberRequest request = new TeamMemberRequest(USER_REFERENCE);

        assertEquals(new TeamMemberCommand(TEAM_REFERENCE, USER_REFERENCE), mapper.toCommand(TEAM_REFERENCE, request));
    }

    @Test
    void combinesTeamAndUserPathReferences() {
        assertEquals(
                new TeamMemberCommand(TEAM_REFERENCE, USER_REFERENCE),
                mapper.toCommand(TEAM_REFERENCE, USER_REFERENCE));
    }

    @Test
    void mapsDomainMembershipStatusAndDatesToResponse() {
        TeamMember member = TeamMember.add(ORGANIZATION_REFERENCE, 1L, USER_REFERENCE, STARTED_AT);
        member.remove(ENDED_AT);

        assertEquals(
                new TeamMemberResponse(USER_REFERENCE, TeamMemberStatus.REMOVED.name(), STARTED_AT, ENDED_AT),
                mapper.toResponse(member));
    }
}
