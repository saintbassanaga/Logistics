-- Customer Shipment Enhancements
-- Adds support for:
-- 1. User password storage and username for local authentication
-- 2. Customer-created shipments with validation workflow
-- 3. CUSTOMER role scope

-- =============================================================================
-- ALTER: platform_user - Add username and password hash for local authentication
-- =============================================================================
ALTER TABLE platform_user
    ADD COLUMN username VARCHAR(50) UNIQUE;

ALTER TABLE platform_user
    ADD COLUMN password_hash VARCHAR(255);

-- Add constraint for username format (alphanumeric, underscores, hyphens, 3-50 chars)
ALTER TABLE platform_user
    ADD CONSTRAINT chk_user_username CHECK (username IS NULL OR username ~ '^[a-zA-Z0-9_-]{3,50}$');

CREATE INDEX idx_user_username ON platform_user (username);

COMMENT ON COLUMN platform_user.username IS 'Unique username for local authentication (optional)';
COMMENT ON COLUMN platform_user.password_hash IS 'BCrypt hashed password for local authentication (optional if using external OAuth only)';

-- =============================================================================
-- ALTER: role - Add CUSTOMER scope
-- =============================================================================
ALTER TABLE role
    DROP CONSTRAINT chk_role_scope;

ALTER TABLE role
    ADD CONSTRAINT chk_role_scope CHECK (scope IN ('AGENCY', 'PLATFORM', 'CUSTOMER'));

-- =============================================================================
-- ALTER: shipment - Add customer tracking and validation workflow
-- =============================================================================

-- Add customer_id to track who created the shipment
ALTER TABLE shipment
    ADD COLUMN customer_id UUID;

-- Add pickup_location_id for validation at pickup
ALTER TABLE shipment
    ADD COLUMN pickup_location_id UUID;

-- Add validated_by_id to track who validated
ALTER TABLE shipment
    ADD COLUMN validated_by_id UUID;

-- Add validated_at timestamp
ALTER TABLE shipment
    ADD COLUMN validated_at TIMESTAMP;

-- Add rejection_reason for rejected shipments
ALTER TABLE shipment
    ADD COLUMN rejection_reason TEXT;

-- Update status constraint to include PENDING_VALIDATION and REJECTED
ALTER TABLE shipment
    DROP CONSTRAINT chk_shipment_status;

ALTER TABLE shipment
    ADD CONSTRAINT chk_shipment_status CHECK (status IN ('PENDING_VALIDATION', 'OPEN', 'CONFIRMED', 'REJECTED'));

-- Foreign key for customer
ALTER TABLE shipment
    ADD CONSTRAINT fk_shipment_customer FOREIGN KEY (customer_id) REFERENCES platform_user (id);

-- Foreign key for pickup location
ALTER TABLE shipment
    ADD CONSTRAINT fk_shipment_pickup_location FOREIGN KEY (pickup_location_id) REFERENCES agency_location (id);

-- Foreign key for validator
ALTER TABLE shipment
    ADD CONSTRAINT fk_shipment_validator FOREIGN KEY (validated_by_id) REFERENCES platform_user (id);

-- =============================================================================
-- INDEXES for new shipment columns
-- =============================================================================
CREATE INDEX idx_shipment_customer_id ON shipment (customer_id);
CREATE INDEX idx_shipment_pickup_location_id ON shipment (pickup_location_id);
CREATE INDEX idx_shipment_validated_by_id ON shipment (validated_by_id);

-- Composite index for customer's pending shipments
CREATE INDEX idx_shipment_customer_pending ON shipment (customer_id, status) WHERE status = 'PENDING_VALIDATION' AND deleted_at IS NULL;

-- Composite index for agency location pending validations
CREATE INDEX idx_shipment_location_pending ON shipment (pickup_location_id, status) WHERE status = 'PENDING_VALIDATION' AND deleted_at IS NULL;

-- =============================================================================
-- COMMENTS for new shipment columns
-- =============================================================================
COMMENT ON COLUMN shipment.customer_id IS 'ID of the customer who created this shipment (null if created by agency employee)';
COMMENT ON COLUMN shipment.pickup_location_id IS 'Agency location where shipment will be picked up for validation';
COMMENT ON COLUMN shipment.validated_by_id IS 'ID of the agency employee who validated this shipment';
COMMENT ON COLUMN shipment.validated_at IS 'Timestamp when shipment was validated';
COMMENT ON COLUMN shipment.rejection_reason IS 'Reason for rejection if shipment was rejected';

-- =============================================================================
-- SEED: Customer-scoped roles
-- =============================================================================
INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'USER', 'Standard User', 'Default role for registered customers', 'CUSTOMER', true);

-- =============================================================================
-- COMMENTS
-- =============================================================================
COMMENT ON TABLE role IS 'Seeded with standard platform and customer roles';