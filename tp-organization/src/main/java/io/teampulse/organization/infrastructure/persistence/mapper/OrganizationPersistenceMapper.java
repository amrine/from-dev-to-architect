package io.teampulse.organization.infrastructure.persistence.mapper;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.time.ZoneId;

@Mapper(config = CommonMapperConfig.class)
public interface OrganizationPersistenceMapper {

    default Organization toDomain(OrganizationEntity entity) {
        if (entity == null) {
            return null;
        }

        return Organization.restore(
            entity.getReference(),
            entity.getName(),
            entity.getTimezone(),
            entity.getAdminReference(),
            entity.getManagerReference(),
            entity.getStatus()
        );
    }

    OrganizationEntity toEntity(Organization organization);

    void updateEntity(
        @MappingTarget OrganizationEntity entity,
        Organization organization
    );

    default String toTimezoneId(ZoneId timezone) {
        return timezone == null ? null : timezone.getId();
    }
}
