CREATE SEQUENCE organizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE organizations (
    id                          BIGINT      NOT NULL DEFAULT nextval('organizations_id_seq'::regclass),
    reference                   TEXT        NOT NULL,
    name                        TEXT        NOT NULL,
    timezone                    TEXT        NOT NULL,
    admin_reference             TEXT,
    manager_reference           TEXT,
    status                      TEXT        NOT NULL,
    version                     BIGINT      NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  TEXT        NOT NULL,
    modified_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by                 TEXT        NOT NULL,

    CONSTRAINT pk_organizations PRIMARY KEY (id),
    CONSTRAINT uk_organizations_reference UNIQUE (reference),
    CONSTRAINT ck_organizations_name_length CHECK (char_length(name) BETWEEN 1 AND 200),
    CONSTRAINT ck_organizations_status CHECK (
        status IN ('CREATING', 'ACTIVE', 'SUSPENDED', 'ARCHIVED')
    ),
    CONSTRAINT ck_organizations_responsibles_by_status CHECK (
           status = 'CREATING'
        OR status = 'ARCHIVED'
        OR (
            status = 'ACTIVE'
            AND admin_reference IS NOT NULL
            AND manager_reference IS NOT NULL
        )
        OR (
            status = 'SUSPENDED'
            AND admin_reference IS NOT NULL
        )
    )
);

ALTER SEQUENCE organizations_id_seq OWNED BY organizations.id;
