-- Remove Local Authentication
-- Migration to remove username and password_hash columns as authentication
-- is now fully delegated to Keycloak (OAuth2/OIDC).
-- The external_auth_id column (Keycloak sub claim) is now the single source
-- of authentication identity.

-- =============================================================================
-- ALTER: platform_user - Make external_auth_id required
-- =============================================================================

-- Note: Before running this migration, ensure all users have an external_auth_id.
-- If there are users without external_auth_id, you need to either:
-- 1. Create corresponding Keycloak users and update external_auth_id
-- 2. Delete orphan users

-- First, let's handle any existing users without external_auth_id
-- by setting it to a placeholder (they won't be able to authenticate until properly linked)
UPDATE platform_user
SET external_auth_id = 'UNLINKED_' || id::text
WHERE external_auth_id IS NULL;

-- Now make external_auth_id NOT NULL
ALTER TABLE platform_user
    ALTER COLUMN external_auth_id SET NOT NULL;

-- =============================================================================
-- COMMENTS
-- =============================================================================
COMMENT ON COLUMN platform_user.external_auth_id IS 'Required Keycloak user ID (sub claim). Users must be created in Keycloak first.';