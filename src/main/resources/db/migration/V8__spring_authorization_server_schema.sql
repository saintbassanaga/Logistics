-- Spring OAuth2 Authorization Server Schema Migration
-- Replaces Keycloak with embedded Spring Authorization Server.
-- Includes:
--   1. Make external_auth_id nullable (no longer required for local Spring AS users)
--   2. Spring AS JDBC persistence tables (official PostgreSQL schema)

-- =============================================================================
-- 1. Revert V7: external_auth_id no longer mandatory for Spring AS users
-- =============================================================================
ALTER TABLE platform_user
    ALTER COLUMN external_auth_id DROP NOT NULL;

COMMENT ON COLUMN platform_user.external_auth_id IS
    'Optional external identity provider ID. Null for users authenticated via the embedded Spring Authorization Server.';

-- =============================================================================
-- 2. Spring Authorization Server - Registered Client Table
-- =============================================================================
CREATE TABLE oauth2_registered_client
(
    id                            VARCHAR(100)  NOT NULL,
    client_id                     VARCHAR(100)  NOT NULL,
    client_id_issued_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_secret                 VARCHAR(200)  DEFAULT NULL,
    client_secret_expires_at      TIMESTAMP     DEFAULT NULL,
    client_name                   VARCHAR(200)  NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris     VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,
    token_settings                VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id)
);

-- =============================================================================
-- 3. Spring Authorization Server - Authorization Table
-- =============================================================================
CREATE TABLE oauth2_authorization
(
    id                            VARCHAR(100)  NOT NULL,
    registered_client_id          VARCHAR(100)  NOT NULL,
    principal_name                VARCHAR(200)  NOT NULL,
    authorization_grant_type      VARCHAR(100)  NOT NULL,
    authorized_scopes             VARCHAR(1000) DEFAULT NULL,
    attributes                    TEXT          DEFAULT NULL,
    state                         VARCHAR(500)  DEFAULT NULL,
    authorization_code_value      TEXT          DEFAULT NULL,
    authorization_code_issued_at  TIMESTAMP     DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP     DEFAULT NULL,
    authorization_code_metadata   TEXT          DEFAULT NULL,
    access_token_value            TEXT          DEFAULT NULL,
    access_token_issued_at        TIMESTAMP     DEFAULT NULL,
    access_token_expires_at       TIMESTAMP     DEFAULT NULL,
    access_token_metadata         TEXT          DEFAULT NULL,
    access_token_type             VARCHAR(100)  DEFAULT NULL,
    access_token_scopes           VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value           TEXT          DEFAULT NULL,
    oidc_id_token_issued_at       TIMESTAMP     DEFAULT NULL,
    oidc_id_token_expires_at      TIMESTAMP     DEFAULT NULL,
    oidc_id_token_metadata        TEXT          DEFAULT NULL,
    refresh_token_value           TEXT          DEFAULT NULL,
    refresh_token_issued_at       TIMESTAMP     DEFAULT NULL,
    refresh_token_expires_at      TIMESTAMP     DEFAULT NULL,
    refresh_token_metadata        TEXT          DEFAULT NULL,
    user_code_value               TEXT          DEFAULT NULL,
    user_code_issued_at           TIMESTAMP     DEFAULT NULL,
    user_code_expires_at          TIMESTAMP     DEFAULT NULL,
    user_code_metadata            TEXT          DEFAULT NULL,
    device_code_value             TEXT          DEFAULT NULL,
    device_code_issued_at         TIMESTAMP     DEFAULT NULL,
    device_code_expires_at        TIMESTAMP     DEFAULT NULL,
    device_code_metadata          TEXT          DEFAULT NULL,
    PRIMARY KEY (id)
);

-- =============================================================================
-- 4. Spring Authorization Server - Authorization Consent Table
-- =============================================================================
CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(200)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- =============================================================================
-- INDEXES
-- =============================================================================
CREATE INDEX idx_oauth2_registered_client_id ON oauth2_registered_client (client_id);
CREATE INDEX idx_oauth2_authorization_principal ON oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_authorization_client ON oauth2_authorization (registered_client_id);

-- =============================================================================
-- COMMENTS
-- =============================================================================
COMMENT ON TABLE oauth2_registered_client IS 'Spring Authorization Server: registered OAuth2/OIDC clients';
COMMENT ON TABLE oauth2_authorization IS 'Spring Authorization Server: authorization codes, access/refresh tokens';
COMMENT ON TABLE oauth2_authorization_consent IS 'Spring Authorization Server: user consent records';
