package io.teampulse.identity.infrastructure.web.user;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.domain.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "reference", source = "userReference")
    UserResponse toResponse(UserLifecycleResult user);
}
