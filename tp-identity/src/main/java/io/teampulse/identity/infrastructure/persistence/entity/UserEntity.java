package io.teampulse.identity.infrastructure.persistence.entity;

import io.teampulse.identity.domain.user.model.UserStatus;
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
@Table(schema = "tp_identity", name = "users")
public class UserEntity extends AbstractAuditableEntity {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_generator")
    @SequenceGenerator(name = "users_id_generator", sequenceName = "users_id_seq", schema = "tp_identity", allocationSize = 1)
    private Long id;

    private String reference;
    private String organizationReference;
    private String email;
    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    private UserStatus status;
}
