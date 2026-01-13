-- Initial Schema
-- Compliant with ADR-006: Multi-tenancy via agency_id

-- UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- TABLE: agency
-- Description: Transport agencies (SaaS platform tenants)
-- =============================================================================
CREATE TABLE agency (
    -- Identity
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        code VARCHAR(50) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        legal_name VARCHAR(255),

    -- Contact
                        email VARCHAR(255) NOT NULL,
                        phone VARCHAR(50),
                        website VARCHAR(255),

    -- Primary Address
                        address_line1 VARCHAR(255),
                        address_line2 VARCHAR(255),
                        city VARCHAR(100),
                        state_region VARCHAR(100),
                        postal_code VARCHAR(20),
                        country VARCHAR(2), -- ISO 3166-1 alpha-2

    -- Business Configuration
                        default_currency VARCHAR(3) NOT NULL DEFAULT 'USD', -- ISO 4217
                        timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
                        locale VARCHAR(10) NOT NULL DEFAULT 'en_US',

    -- Legal and Regulatory Information
                        tax_id VARCHAR(50),
                        vat_number VARCHAR(50),
                        transport_license_number VARCHAR(100),

    -- Limits and Quotas (for subscription management)
                        max_shipments_per_month INTEGER,
                        max_users INTEGER,
                        subscription_tier VARCHAR(50) NOT NULL DEFAULT 'BASIC',

    -- Status and Lifecycle
                        active BOOLEAN NOT NULL DEFAULT true,
                        suspended BOOLEAN NOT NULL DEFAULT false,
                        suspension_reason TEXT,

    -- Metadata
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        deleted_at TIMESTAMP, -- Soft delete

    -- Constraints
                        CONSTRAINT chk_agency_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_agency_country CHECK (country IS NULL OR LENGTH(country) = 2),
    CONSTRAINT chk_agency_currency CHECK (LENGTH(default_currency) = 3),
    CONSTRAINT chk_agency_subscription_tier CHECK (
        subscription_tier IN ('BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM')
    )
);

-- =============================================================================
-- INDEXES
-- =============================================================================
CREATE INDEX idx_agency_code ON agency(code);
CREATE INDEX idx_agency_active ON agency(active);
CREATE INDEX idx_agency_email ON agency(email);
CREATE INDEX idx_agency_subscription_tier ON agency(subscription_tier);
CREATE INDEX idx_agency_country ON agency(country);
CREATE INDEX idx_agency_deleted_at ON agency(deleted_at);

-- Composite index for common searches
CREATE INDEX idx_agency_active_not_deleted ON agency(active, deleted_at)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- COMMENTS
-- =============================================================================
COMMENT ON TABLE agency IS 'Transport agencies (SaaS platform tenants)';

-- Identity
COMMENT ON COLUMN agency.id IS 'Unique identifier for the agency (tenant_id)';
COMMENT ON COLUMN agency.code IS 'Unique short code for the agency (e.g., ACME-TRANS)';
COMMENT ON COLUMN agency.name IS 'Commercial name of the agency';
COMMENT ON COLUMN agency.legal_name IS 'Official registered company name';

-- Contact
COMMENT ON COLUMN agency.email IS 'Primary contact email';
COMMENT ON COLUMN agency.phone IS 'Primary phone number';
COMMENT ON COLUMN agency.website IS 'Agency website';

-- Address
COMMENT ON COLUMN agency.address_line1 IS 'Address line 1';
COMMENT ON COLUMN agency.address_line2 IS 'Address line 2 (supplementary)';
COMMENT ON COLUMN agency.city IS 'City';
COMMENT ON COLUMN agency.state_region IS 'State/Region/Province';
COMMENT ON COLUMN agency.postal_code IS 'Postal code';
COMMENT ON COLUMN agency.country IS 'ISO 3166-1 alpha-2 country code';

-- Configuration
COMMENT ON COLUMN agency.default_currency IS 'Default currency (ISO 4217)';
COMMENT ON COLUMN agency.timezone IS 'Default timezone (IANA timezone)';
COMMENT ON COLUMN agency.locale IS 'Default locale (language_COUNTRY)';

-- Legal
COMMENT ON COLUMN agency.tax_id IS 'Tax identification number';
COMMENT ON COLUMN agency.vat_number IS 'VAT number (Intra-community)';
COMMENT ON COLUMN agency.transport_license_number IS 'Transport license number';

-- Quotas
COMMENT ON COLUMN agency.max_shipments_per_month IS 'Maximum shipments per month (null = unlimited)';
COMMENT ON COLUMN agency.max_users IS 'Maximum number of users (null = unlimited)';
COMMENT ON COLUMN agency.subscription_tier IS 'Subscription level';

-- Status
COMMENT ON COLUMN agency.active IS 'Active agency (can perform operations)';
COMMENT ON COLUMN agency.suspended IS 'Temporarily suspended agency';
COMMENT ON COLUMN agency.suspension_reason IS 'Reason for suspension';

-- Metadata
COMMENT ON COLUMN agency.created_at IS 'Creation date';
COMMENT ON COLUMN agency.updated_at IS 'Last update date';
COMMENT ON COLUMN agency.deleted_at IS 'Deletion date (soft delete)';

-- =============================================================================
-- TRIGGER: updated_at auto-update
-- =============================================================================
CREATE OR REPLACE FUNCTION update_agency_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_agency_updated_at
    BEFORE UPDATE ON agency
    FOR EACH ROW
    EXECUTE FUNCTION update_agency_updated_at();
