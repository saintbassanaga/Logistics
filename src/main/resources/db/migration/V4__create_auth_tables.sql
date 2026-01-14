-- Auth Context Schema
-- Compliant with ADR-003: Auth as source of truth
-- Compliant with ADR-004: Rich JWT with actor_type, agency_id, roles

-- =============================================================================
-- TABLE: platform_user
-- Description: Users of the platform (source of truth for identity)
-- =============================================================================
CREATE TABLE platform_user
(
    -- Identity
    id               UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    email            VARCHAR(255) NOT NULL UNIQUE,
    first_name       VARCHAR(255) NOT NULL,
    last_name        VARCHAR(255) NOT NULL,
    phone            VARCHAR(50),

    -- Actor Type (determines permissions scope)
    actor_type       VARCHAR(20)  NOT NULL,

    -- Agency ID (only for AGENCY_EMPLOYEE)
    agency_id        UUID,

    -- External Auth Integration (Keycloak/OAuth)
    external_auth_id VARCHAR(255) UNIQUE,

    -- Employment Details (for AGENCY_EMPLOYEE)
    job_title        VARCHAR(100),
    department       VARCHAR(100),

    -- Status
    active           BOOLEAN      NOT NULL DEFAULT true,
    email_verified   BOOLEAN      NOT NULL DEFAULT false,

    -- Metadata
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at    TIMESTAMP,
    deleted_at       TIMESTAMP, -- Soft delete

    -- Foreign Keys
    CONSTRAINT fk_user_agency FOREIGN KEY (agency_id) REFERENCES agency (id),

    -- Constraints
    CONSTRAINT chk_user_actor_type CHECK (actor_type IN ('AGENCY_EMPLOYEE', 'CUSTOMER', 'PLATFORM_ADMIN')),
    CONSTRAINT chk_user_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
) ,
    CONSTRAINT chk_user_agency_consistency CHECK (
        (actor_type = 'AGENCY_EMPLOYEE' AND agency_id IS NOT NULL) OR
        (actor_type IN ('PLATFORM_ADMIN', 'CUSTOMER') AND agency_id IS NULL)
    )
);

-- =============================================================================
-- INDEXES for platform_user
-- =============================================================================
CREATE INDEX idx_user_email ON platform_user (email);
CREATE INDEX idx_user_external_auth_id ON platform_user (external_auth_id);
CREATE INDEX idx_user_actor_type ON platform_user (actor_type);
CREATE INDEX idx_user_agency_id ON platform_user (agency_id);
CREATE INDEX idx_user_active ON platform_user (active);
CREATE INDEX idx_user_deleted_at ON platform_user (deleted_at);

-- Composite index for active agency employees
CREATE INDEX idx_user_agency_active ON platform_user (agency_id, actor_type, active) WHERE actor_type = 'AGENCY_EMPLOYEE' AND deleted_at IS NULL;

-- =============================================================================
-- COMMENTS for platform_user
-- =============================================================================
COMMENT
ON TABLE platform_user IS 'Platform users (source of truth for identity and authorization)';
COMMENT
ON COLUMN platform_user.id IS 'Unique identifier (used as JWT sub)';
COMMENT
ON COLUMN platform_user.email IS 'Unique email address';
COMMENT
ON COLUMN platform_user.actor_type IS 'Actor type (AGENCY_EMPLOYEE, CUSTOMER, PLATFORM_ADMIN)';
COMMENT
ON COLUMN platform_user.agency_id IS 'Agency ID (only for AGENCY_EMPLOYEE, populates JWT agency_id claim)';
COMMENT
ON COLUMN platform_user.external_auth_id IS 'External OAuth identity provider ID (Keycloak sub)';
COMMENT
ON COLUMN platform_user.active IS 'Account active status';
COMMENT
ON COLUMN platform_user.email_verified IS 'Email verification status';

-- =============================================================================
-- TRIGGER: updated_at auto-update for platform_user
-- =============================================================================
CREATE
OR REPLACE FUNCTION update_user_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_user_updated_at
    BEFORE UPDATE
    ON platform_user
    FOR EACH ROW
    EXECUTE FUNCTION update_user_updated_at();

-- =============================================================================
-- TABLE: role
-- Description: Application roles (permissions)
-- =============================================================================
CREATE TABLE role
(
    -- Identity
    id          UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,

    -- Scope
    scope       VARCHAR(20)  NOT NULL DEFAULT 'AGENCY',

    -- Status
    active      BOOLEAN      NOT NULL DEFAULT true,

    -- Metadata
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_role_scope CHECK (scope IN ('AGENCY', 'PLATFORM')),
    CONSTRAINT chk_role_code_format CHECK (code ~ '^[A-Z_]+$'
)
    );

-- =============================================================================
-- INDEXES for role
-- =============================================================================
CREATE INDEX idx_role_code ON role (code);
CREATE INDEX idx_role_scope ON role (scope);
CREATE INDEX idx_role_active ON role (active);

-- =============================================================================
-- COMMENTS for role
-- =============================================================================
COMMENT
ON TABLE role IS 'Application roles (functional permissions)';
COMMENT
ON COLUMN role.code IS 'Unique role code (uppercase with underscores)';
COMMENT
ON COLUMN role.name IS 'Human-readable role name';
COMMENT
ON COLUMN role.scope IS 'Role scope (AGENCY or PLATFORM)';

-- =============================================================================
-- TRIGGER: updated_at auto-update for role
-- =============================================================================
CREATE
OR REPLACE FUNCTION update_role_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_role_updated_at
    BEFORE UPDATE
    ON role
    FOR EACH ROW
    EXECUTE FUNCTION update_role_updated_at();

-- =============================================================================
-- TABLE: user_role (join table)
-- Description: User-Role assignments
-- =============================================================================
CREATE TABLE user_role
(
    user_id     UUID      NOT NULL,
    role_id     UUID      NOT NULL,

    -- Metadata
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    -- Foreign Keys
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES platform_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);

-- =============================================================================
-- INDEXES for user_role
-- =============================================================================
CREATE INDEX idx_user_role_user_id ON user_role (user_id);
CREATE INDEX idx_user_role_role_id ON user_role (role_id);

-- =============================================================================
-- COMMENTS for user_role
-- =============================================================================
COMMENT
ON TABLE user_role IS 'User-Role assignments (populates JWT roles claim)';
COMMENT
ON COLUMN user_role.user_id IS 'User ID';
COMMENT
ON COLUMN user_role.role_id IS 'Role ID';
COMMENT
ON COLUMN user_role.assigned_at IS 'Role assignment timestamp';
