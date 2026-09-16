package io.teampulse.team.infrastructure.persistence.entity;

import io.teampulse.team.domain.team.model.TeamStatus;
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
@Table(schema = "tp_team", name = "teams")
public class TeamEntity extends AbstractAuditableEntity {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "teams_id_generator")
    @SequenceGenerator(
            name = "teams_id_generator",
            sequenceName = "teams_id_seq",
            schema = "tp_team",
            allocationSize = 1)
    private Long id;

    private String reference;
    private String organizationReference;
    private String name;
    private String adminReference;
    private String managerReference;

    @Enumerated(EnumType.STRING)
    private TeamStatus status;
}
