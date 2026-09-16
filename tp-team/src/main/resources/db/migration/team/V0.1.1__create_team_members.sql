CREATE SEQUENCE team_members_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE team_members
(
    id                     BIGINT      NOT NULL DEFAULT nextval('team_members_id_seq'::regclass),
    organization_reference TEXT        NOT NULL,
    team_id                BIGINT      NOT NULL,
    user_reference         TEXT        NOT NULL,
    status                 TEXT        NOT NULL,
    started_at             TIMESTAMPTZ,
    ended_at               TIMESTAMPTZ,
    version                BIGINT      NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             TEXT        NOT NULL,
    modified_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by            TEXT        NOT NULL,

    CONSTRAINT pk_team_members PRIMARY KEY (id),
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id, organization_reference)
        REFERENCES teams (id, organization_reference),
    CONSTRAINT ck_team_members_status CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED')),
    CONSTRAINT ck_team_members_dates_by_status CHECK (
        (status = 'INVITED'
            AND started_at IS NULL
            AND ended_at IS NULL)
            OR (status IN ('ACTIVE', 'SUSPENDED')
            AND started_at IS NOT NULL
            AND ended_at IS NULL)
            OR (status = 'REMOVED'
            AND ended_at IS NOT NULL)
        ),
    CONSTRAINT ck_team_members_ended_at_after_started_at CHECK (
        ended_at IS NULL
            OR started_at IS NULL
            OR ended_at >= started_at
        )
);

ALTER SEQUENCE team_members_id_seq OWNED BY team_members.id;

CREATE UNIQUE INDEX uk_team_members_current
    ON team_members (organization_reference, team_id, user_reference)
    WHERE status IN ('INVITED', 'ACTIVE', 'SUSPENDED');
