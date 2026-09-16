package io.teampulse.team.infrastructure.persistence.mapper;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = CommonMapperConfig.class)
public interface TeamMemberPersistenceMapper {

    default TeamMember toDomain(TeamMemberEntity entity) {
        if (entity == null) {
            return null;
        }

        return TeamMember.restore(
                entity.getId(),
                entity.getOrganizationReference(),
                entity.getTeamId(),
                entity.getUserReference(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getEndedAt());
    }

    TeamMemberEntity toEntity(TeamMember teamMember);

    void updateEntity(@MappingTarget TeamMemberEntity entity, TeamMember teamMember);
}
