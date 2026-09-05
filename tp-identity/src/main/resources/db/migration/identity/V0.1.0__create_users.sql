CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE users (
    id                          BIGINT      NOT NULL    DEFAULT nextval('users_id_seq'::regclass),
    reference                   TEXT        NOT NULL,
    organization_reference      TEXT        NOT NULL,
    email                       TEXT        NOT NULL,
    first_name                  TEXT        NOT NULL,
    last_name                   TEXT        NOT NULL,
    status                      TEXT        NOT NULL,
    version                     BIGINT      NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  TEXT        NOT NULL,
    modified_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by                 TEXT        NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_reference UNIQUE (reference),
    CONSTRAINT uk_users_organization_email UNIQUE (organization_reference, email),
    CONSTRAINT ck_users_email_length CHECK (char_length(email) <= 254),
    CONSTRAINT ck_users_first_name_length CHECK (char_length(first_name) <= 100),
    CONSTRAINT ck_users_last_name_length CHECK (char_length(last_name) <= 100),
    CONSTRAINT ck_users_status CHECK (status IN ('INVITED','CREATING','ACTIVE','SUSPENDED','DEACTIVATED'))
);

ALTER SEQUENCE users_id_seq OWNED BY users.id;
CREATE INDEX idx_users_organization_reference ON users (organization_reference, reference);