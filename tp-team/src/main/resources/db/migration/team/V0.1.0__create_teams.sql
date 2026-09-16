CREATE SEQUENCE teams_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE teams
(
    id                     BIGINT      NOT NULL DEFAULT nextval('teams_id_seq'::regclass),
    reference              TEXT        NOT NULL,
    organization_reference TEXT        NOT NULL,
    name                   TEXT        NOT NULL,
    admin_reference        TEXT        NOT NULL,
    manager_reference      TEXT        NOT NULL,
    status                 TEXT        NOT NULL,
    version                BIGINT      NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             TEXT        NOT NULL,
    modified_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by            TEXT        NOT NULL,

    CONSTRAINT pk_teams PRIMARY KEY (id),
    CONSTRAINT uk_teams_reference UNIQUE (reference),
    CONSTRAINT uk_teams_id_organization_reference UNIQUE (id, organization_reference),
    CONSTRAINT ck_teams_name_length CHECK (char_length(name) BETWEEN 1 AND 200),
    CONSTRAINT ck_teams_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

ALTER SEQUENCE teams_id_seq OWNED BY teams.id;

CREATE INDEX idx_teams_organization_reference ON teams (organization_reference, reference);
