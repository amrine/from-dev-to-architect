package io.teampulse.team.infrastructure.web.team;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.domain.team.model.Team;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface TeamMapper {

    @Mapping(target = "adminReference", source = "administratorReference")
    CreateTeamCommand toCommand(CreateTeamRequest request);

    @Mapping(target = "responsibleReference", source = "request.userReference")
    TeamResponsibleCommand toResponsibleCommand(String teamReference, TeamResponsibleUserRequest request);

    @Mapping(target = "administratorReference", source = "adminReference")
    TeamResponse toResponse(Team team);
}
