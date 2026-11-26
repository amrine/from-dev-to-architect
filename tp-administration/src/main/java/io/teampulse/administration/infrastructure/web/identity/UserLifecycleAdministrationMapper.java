package io.teampulse.administration.infrastructure.web.identity;

import io.teampulse.administration.application.port.in.identity.UserLifecycleAdministrationResult;
import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import org.mapstruct.Mapper;

@Mapper(config = CommonMapperConfig.class)
public interface UserLifecycleAdministrationMapper {

    UserProvisioningCommand toCommand(UserLifecycleAdministrationRequest request);

    UserLifecycleAdministrationResponse toResponse(UserLifecycleAdministrationResult result);
}
