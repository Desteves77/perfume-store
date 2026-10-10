CREATE TABLE perfumestore.app_users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL,
    email_normalized VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    password_changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_users_role_ck CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT app_users_status_ck CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX app_users_status_idx ON perfumestore.app_users (status);

CREATE TABLE perfumestore.password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES perfumestore.app_users (id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX password_reset_tokens_user_idx
    ON perfumestore.password_reset_tokens (user_id, expires_at);

CREATE INDEX password_reset_tokens_expiry_idx
    ON perfumestore.password_reset_tokens (expires_at)
    WHERE used_at IS NULL;

CREATE TABLE perfumestore.spring_session (
    primary_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON perfumestore.spring_session (session_id);
CREATE INDEX spring_session_ix2 ON perfumestore.spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON perfumestore.spring_session (principal_name);

CREATE TABLE perfumestore.spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES perfumestore.spring_session (primary_id) ON DELETE CASCADE
);
