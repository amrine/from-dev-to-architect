package io.teampulse.team.infrastructure.persistence.entity;

import io.teampulse.team.domain.team.model.TeamMemberStatus;
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

import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Accessors(chain = true)
@Table(schema = "tp_team", name = "team_members")
public class TeamMemberEntity extends AbstractAuditableEntity {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_members_id_generator")
    @SequenceGenerator(
            name = "team_members_id_generator",
            sequenceName = "team_members_id_seq",
            schema = "tp_team",
            allocationSize = 1)
    private Long id;

    private String organizationReference;
    private Long teamId;
    private String userReference;

    @Enumerated(EnumType.STRING)
    private TeamMemberStatus status;

    private Instant startedAt;
    private Instant endedAt;
}
