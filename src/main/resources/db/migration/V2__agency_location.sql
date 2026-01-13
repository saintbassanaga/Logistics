-- Migration V2: Agency Locations
-- An agency can have multiple locations (representations in multiple cities)

-- =============================================================================
-- TABLE: agency_location
-- Description: Locations/branches of transport agencies
-- =============================================================================
CREATE TABLE agency_location (
    -- Identity
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 agency_id UUID NOT NULL REFERENCES agency(id) ON DELETE CASCADE,

    -- Location Identification
                                 code VARCHAR(50) NOT NULL,
                                 name VARCHAR(255) NOT NULL,
                                 location_type VARCHAR(50) NOT NULL DEFAULT 'BRANCH',

    -- Full Address
                                 address_line1 VARCHAR(255) NOT NULL,
                                 address_line2 VARCHAR(255),
                                 city VARCHAR(100) NOT NULL,
                                 state_region VARCHAR(100),
                                 postal_code VARCHAR(20) NOT NULL,
                                 country VARCHAR(2) NOT NULL, -- ISO 3166-1 alpha-2

    -- Geographic Coordinates (for mapping and distance calculations)
                                 latitude DECIMAL(10, 8),
                                 longitude DECIMAL(11, 8),

    -- Location-Specific Contact Info
                                 email VARCHAR(255),
                                 phone VARCHAR(50) NOT NULL,
                                 contact_person_name VARCHAR(255),
                                 contact_person_phone VARCHAR(50),

    -- Operational Hours and Capabilities
                                 opening_hours TEXT, -- JSON format: {"monday": "08:00-18:00", ...}
                                 is_pickup_point BOOLEAN NOT NULL DEFAULT true,
                                 is_delivery_point BOOLEAN NOT NULL DEFAULT true,
                                 is_warehouse BOOLEAN NOT NULL DEFAULT false,

    -- Capacities
                                 max_daily_parcels INTEGER,
                                 storage_capacity_m3 DECIMAL(10, 2),

    -- Status
                                 active BOOLEAN NOT NULL DEFAULT true,
                                 temporarily_closed BOOLEAN NOT NULL DEFAULT false,
                                 closure_reason TEXT,

    -- Metadata
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 deleted_at TIMESTAMP, -- Soft delete

    -- Constraints
                                 CONSTRAINT uq_agency_location_code UNIQUE (agency_id, code),
                                 CONSTRAINT chk_location_country CHECK (LENGTH(country) = 2),
                                 CONSTRAINT chk_location_type CHECK (
                                     location_type IN ('HEADQUARTERS', 'BRANCH', 'WAREHOUSE', 'PICKUP_POINT', 'SORTING_CENTER')
                                     ),
                                 CONSTRAINT chk_location_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
                                 CONSTRAINT chk_location_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

-- =============================================================================
-- INDEXES
-- =============================================================================

-- Index on agency_id (FK, frequent queries per agency)
CREATE INDEX idx_agency_location_agency_id ON agency_location(agency_id);

-- Index on code (searches)
CREATE INDEX idx_agency_location_code ON agency_location(code);

-- Index on city (geographic searches)
CREATE INDEX idx_agency_location_city ON agency_location(city);

-- Index on country
CREATE INDEX idx_agency_location_country ON agency_location(country);

-- Index on active status
CREATE INDEX idx_agency_location_active ON agency_location(active);

-- Composite index for active, non-deleted locations
CREATE INDEX idx_agency_location_active_not_deleted
    ON agency_location(agency_id, active, deleted_at)
    WHERE deleted_at IS NULL;

-- Geospatial index for coordinate-based searches
CREATE INDEX idx_agency_location_coordinates
    ON agency_location(latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Index on location type
CREATE INDEX idx_agency_location_type ON agency_location(location_type);

-- Composite index for searching active pickup/delivery points
CREATE INDEX idx_agency_location_pickup_delivery
    ON agency_location(agency_id, is_pickup_point, is_delivery_point, active)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON TABLE agency_location IS 'Locations/branches of agencies (multiple per agency)';

-- Identity
COMMENT ON COLUMN agency_location.id IS 'Unique location identifier';
COMMENT ON COLUMN agency_location.agency_id IS 'Reference to the owner agency (tenant)';
COMMENT ON COLUMN agency_location.code IS 'Short location code (unique per agency)';
COMMENT ON COLUMN agency_location.name IS 'Name of the location';
COMMENT ON COLUMN agency_location.location_type IS 'Type of location';

-- Address
COMMENT ON COLUMN agency_location.address_line1 IS 'Address line 1';
COMMENT ON COLUMN agency_location.address_line2 IS 'Address line 2 (supplementary)';
COMMENT ON COLUMN agency_location.city IS 'City';
COMMENT ON COLUMN agency_location.state_region IS 'State/Region/Province';
COMMENT ON COLUMN agency_location.postal_code IS 'Postal code';
COMMENT ON COLUMN agency_location.country IS 'ISO 3166-1 alpha-2 country code';

-- Coordinates
COMMENT ON COLUMN agency_location.latitude IS 'Latitude (WGS84)';
COMMENT ON COLUMN agency_location.longitude IS 'Longitude (WGS84)';

-- Contact
COMMENT ON COLUMN agency_location.email IS 'Contact email for this location';
COMMENT ON COLUMN agency_location.phone IS 'Primary phone number';
COMMENT ON COLUMN agency_location.contact_person_name IS 'Name of the location manager';
COMMENT ON COLUMN agency_location.contact_person_phone IS 'Phone number of the manager';

-- Operational
COMMENT ON COLUMN agency_location.opening_hours IS 'Opening hours (JSON format)';
COMMENT ON COLUMN agency_location.is_pickup_point IS 'Is a collection point (pickup)';
COMMENT ON COLUMN agency_location.is_delivery_point IS 'Is a delivery point';
COMMENT ON COLUMN agency_location.is_warehouse IS 'Is a storage warehouse';

-- Capacities
COMMENT ON COLUMN agency_location.max_daily_parcels IS 'Daily capacity in number of parcels';
COMMENT ON COLUMN agency_location.storage_capacity_m3 IS 'Storage capacity in m³';

-- Status
COMMENT ON COLUMN agency_location.active IS 'Location is active';
COMMENT ON COLUMN agency_location.temporarily_closed IS 'Location is temporarily closed';
COMMENT ON COLUMN agency_location.closure_reason IS 'Reason for closure';

-- Metadata
COMMENT ON COLUMN agency_location.created_at IS 'Creation date';
COMMENT ON COLUMN agency_location.updated_at IS 'Last update date';
COMMENT ON COLUMN agency_location.deleted_at IS 'Deletion date (soft delete)';

-- =============================================================================
-- TRIGGER: updated_at auto-update
-- =============================================================================

CREATE OR REPLACE FUNCTION update_agency_location_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_agency_location_updated_at
    BEFORE UPDATE ON agency_location
    FOR EACH ROW
    EXECUTE FUNCTION update_agency_location_updated_at();
