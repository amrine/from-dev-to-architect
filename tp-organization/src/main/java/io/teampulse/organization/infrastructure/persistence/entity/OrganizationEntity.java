package io.teampulse.organization.infrastructure.persistence.entity;

import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Accessors(chain = true)
@Table(schema = "tp_organization", name = "organizations")
public class OrganizationEntity extends AbstractAuditableEntity {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "organizations_id_generator"
    )
    @SequenceGenerator(
        name = "organizations_id_generator",
        sequenceName = "organizations_id_seq",
        schema = "tp_organization",
        allocationSize = 1
    )
    private Long id;

    private String reference;
    private String name;
    private String timezone;
    private String adminReference;
    private String managerReference;

    @Enumerated(EnumType.STRING)
    private OrganizationStatus status;
}
