package io.teampulse.team.infrastructure.persistence.mapper;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = CommonMapperConfig.class)
public interface TeamPersistenceMapper {

    default Team toDomain(TeamEntity entity) {
        if (entity == null) {
            return null;
        }

        return Team.restore(
                entity.getId(),
                entity.getReference(),
                entity.getOrganizationReference(),
                entity.getName(),
                entity.getAdminReference(),
                entity.getManagerReference(),
                entity.getStatus());
    }

    TeamEntity toEntity(Team team);

    void updateEntity(@MappingTarget TeamEntity entity, Team team);
}
