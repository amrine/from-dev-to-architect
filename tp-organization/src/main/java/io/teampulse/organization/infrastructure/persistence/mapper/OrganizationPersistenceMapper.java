package io.teampulse.organization.infrastructure.persistence.mapper;

import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.time.ZoneId;

@Mapper(config = CommonMapperConfig.class)
public interface OrganizationPersistenceMapper {

    /**
     * Restores the domain model from stored business fields and revalidates its invariants.
     * Technical identity, locking version and audit metadata stay on the entity.
     *
     * @param entity persisted representation, possibly null
     * @return the restored model, or null for a null entity
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         if stored business data violates domain invariants
     */
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

    /**
     * Copies business fields into a new persistence entity.
     * The database and auditing infrastructure supply its technical metadata.
     *
     * @param organization domain model to persist
     * @return a new entity, or null for a null model
     */
    OrganizationEntity toEntity(Organization organization);

    /**
     * Copies business state onto an existing managed entity.
     * Its technical identifier, locking version and audit metadata are retained.
     *
     * @param entity persistence entity to update in place
     * @param organization source business state
     */
    void updateEntity(
        @MappingTarget OrganizationEntity entity,
        Organization organization
    );

    /**
     * Converts a timezone into the identifier stored in PostgreSQL.
     *
     * @param timezone domain timezone, possibly null
     * @return its identifier, or null when no timezone is supplied
     */
    default String toTimezoneId(ZoneId timezone) {
        return timezone == null ? null : timezone.getId();
    }
}
