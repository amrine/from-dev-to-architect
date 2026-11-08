package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleResult;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.domain.organization.model.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface OrganizationMapper {

    /**
     * Combines the platform route's organization reference with the requested user.
     *
     * @param organizationReference target organization from the platform route
     * @param request candidate user reference from the request body
     * @return the responsibility command
     */
    @Mapping(target = "responsibleReference", source = "request.userReference")
    OrganizationResponsibleCommand toCommand(
        String organizationReference, AssignOrganizationResponsibleUserRequest request
    );

    /**
     * Converts an internal domain result into the HTTP representation.
     * Timezone and status are serialized textually; technical persistence fields are omitted.
     *
     * @param organization result of an internal responsibility operation
     * @return the HTTP response, or null for a null model
     */
    @Mapping(target = "administratorReference", source = "adminReference")
    @Mapping(target = "timezone", expression = "java(organization.getTimezone().getId())")
    OrganizationResponse toResponse(Organization organization);

    /**
     * Converts the public lifecycle result into the HTTP representation.
     *
     * @param organization result returned by the public lifecycle contract
     * @return the HTTP response, or null for a null result
     */
    @Mapping(target = "reference", source = "organizationReference")
    OrganizationResponse toResponse(OrganizationLifecycleResult organization);

}
