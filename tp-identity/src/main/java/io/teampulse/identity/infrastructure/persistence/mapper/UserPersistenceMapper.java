package io.teampulse.identity.infrastructure.persistence.mapper;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = CommonMapperConfig.class)
public interface UserPersistenceMapper {

    default User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.restore(
            entity.getReference(),
            entity.getOrganizationReference(),
            entity.getEmail(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getStatus()
        );
    }

    UserEntity toEntity(User user);

    void updateEntity(@MappingTarget UserEntity userEntity, User user);
}
