package io.teampulse.team.infrastructure.web.teammember;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.team.application.port.in.teammember.TeamMemberCommand;
import io.teampulse.team.domain.team.model.TeamMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface TeamMemberMapper {

    TeamMemberCommand toCommand(String teamReference, String userReference);

    @Mapping(target = "userReference", source = "request.userReference")
    TeamMemberCommand toCommand(String teamReference, TeamMemberRequest request);

    TeamMemberResponse toResponse(TeamMember member);
}
