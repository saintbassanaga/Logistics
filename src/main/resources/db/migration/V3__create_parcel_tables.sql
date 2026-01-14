-- Parcel Context Schema
-- Compliant with ADR-006: Multi-tenancy via agency_id
-- Compliant with ADR-007: Shipment as distinct aggregate from Parcel

-- =============================================================================
-- TABLE: shipment
-- Description: Shipments (grouped shipping operations)
-- =============================================================================
CREATE TABLE shipment
(
    -- Identity
    id                     UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    agency_id              UUID         NOT NULL,
    shipment_number        VARCHAR(50)  NOT NULL UNIQUE,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'OPEN',

    -- Sender Information
    sender_name            VARCHAR(255) NOT NULL,
    sender_phone           VARCHAR(50),
    sender_email           VARCHAR(255),
    sender_address_line1   VARCHAR(255) NOT NULL,
    sender_address_line2   VARCHAR(255),
    sender_city            VARCHAR(100) NOT NULL,
    sender_postal_code     VARCHAR(20)  NOT NULL,
    sender_country         VARCHAR(2)   NOT NULL, -- ISO 3166-1 alpha-2

    -- Receiver Information
    receiver_name          VARCHAR(255) NOT NULL,
    receiver_phone         VARCHAR(50),
    receiver_email         VARCHAR(255),
    receiver_address_line1 VARCHAR(255) NOT NULL,
    receiver_address_line2 VARCHAR(255),
    receiver_city          VARCHAR(100) NOT NULL,
    receiver_postal_code   VARCHAR(20)  NOT NULL,
    receiver_country       VARCHAR(2)   NOT NULL, -- ISO 3166-1 alpha-2

    -- Commercial Information
    total_weight           DECIMAL(10, 3),        -- in kg
    declared_value         DECIMAL(15, 2),
    currency               VARCHAR(3),            -- ISO 4217
    notes                  TEXT,

    -- Metadata
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at           TIMESTAMP,
    deleted_at             TIMESTAMP,             -- Soft delete

    -- Foreign Keys
    CONSTRAINT fk_shipment_agency FOREIGN KEY (agency_id) REFERENCES agency (id),

    -- Constraints
    CONSTRAINT chk_shipment_status CHECK (status IN ('OPEN', 'CONFIRMED')),
    CONSTRAINT chk_shipment_sender_country CHECK (LENGTH(sender_country) = 2),
    CONSTRAINT chk_shipment_receiver_country CHECK (LENGTH(receiver_country) = 2),
    CONSTRAINT chk_shipment_currency CHECK (currency IS NULL OR LENGTH(currency) = 3)
);

-- =============================================================================
-- INDEXES for shipment
-- =============================================================================
CREATE INDEX idx_shipment_agency_id ON shipment (agency_id);
CREATE INDEX idx_shipment_number ON shipment (shipment_number);
CREATE INDEX idx_shipment_status ON shipment (status);
CREATE INDEX idx_shipment_agency_status ON shipment (agency_id, status);
CREATE INDEX idx_shipment_deleted_at ON shipment (deleted_at);

-- Composite index for active shipments
CREATE INDEX idx_shipment_active ON shipment (agency_id, status, deleted_at) WHERE deleted_at IS NULL;

-- =============================================================================
-- COMMENTS for shipment
-- =============================================================================
COMMENT
ON TABLE shipment IS 'Shipments (grouped shipping operations)';
COMMENT
ON COLUMN shipment.id IS 'Unique identifier for the shipment';
COMMENT
ON COLUMN shipment.agency_id IS 'Agency owning this shipment (tenant isolation)';
COMMENT
ON COLUMN shipment.shipment_number IS 'Unique shipment number';
COMMENT
ON COLUMN shipment.status IS 'Shipment status (OPEN, CONFIRMED)';
COMMENT
ON COLUMN shipment.total_weight IS 'Total weight of all parcels (kg)';
COMMENT
ON COLUMN shipment.declared_value IS 'Total declared value';
COMMENT
ON COLUMN shipment.created_at IS 'Creation timestamp';
COMMENT
ON COLUMN shipment.updated_at IS 'Last update timestamp';
COMMENT
ON COLUMN shipment.confirmed_at IS 'Confirmation timestamp';
COMMENT
ON COLUMN shipment.deleted_at IS 'Soft delete timestamp';

-- =============================================================================
-- TRIGGER: updated_at auto-update for shipment
-- =============================================================================
CREATE
OR REPLACE FUNCTION update_shipment_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_shipment_updated_at
    BEFORE UPDATE
    ON shipment
    FOR EACH ROW
    EXECUTE FUNCTION update_shipment_updated_at();

-- =============================================================================
-- TABLE: parcel
-- Description: Individual parcels (operational tracking units)
-- =============================================================================
CREATE TABLE parcel
(
    -- Identity
    id                        UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    agency_id                 UUID        NOT NULL,
    shipment_id               UUID        NOT NULL,
    tracking_number           VARCHAR(50) NOT NULL UNIQUE,
    status                    VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',

    -- Physical Dimensions
    weight                    DECIMAL(10, 3), -- in kg
    length                    DECIMAL(10, 2), -- in cm
    width                     DECIMAL(10, 2), -- in cm
    height                    DECIMAL(10, 2), -- in cm

    -- Commercial Information
    description               TEXT,
    declared_value            DECIMAL(15, 2),
    currency                  VARCHAR(3),     -- ISO 4217

    -- Specific Receiver (can differ from shipment)
    specific_receiver_name    VARCHAR(255),
    specific_receiver_phone   VARCHAR(50),
    specific_receiver_address VARCHAR(500),

    -- Tracking
    current_location_id       UUID,           -- Reference to agency_location
    last_scan_at              TIMESTAMP,
    notes                     TEXT,

    -- Metadata
    created_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at              TIMESTAMP,
    deleted_at                TIMESTAMP,      -- Soft delete

    -- Foreign Keys
    CONSTRAINT fk_parcel_agency FOREIGN KEY (agency_id) REFERENCES agency (id),
    CONSTRAINT fk_parcel_shipment FOREIGN KEY (shipment_id) REFERENCES shipment (id),

    -- Constraints
    CONSTRAINT chk_parcel_status CHECK (status IN (
                                                   'REGISTERED', 'IN_TRANSIT', 'IN_SORTING',
                                                   'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'RETURNED'
        )),
    CONSTRAINT chk_parcel_weight CHECK (weight IS NULL OR weight > 0),
    CONSTRAINT chk_parcel_dimensions CHECK (
        (length IS NULL OR length > 0) AND
        (width IS NULL OR width > 0) AND
        (height IS NULL OR height > 0)
        ),
    CONSTRAINT chk_parcel_currency CHECK (currency IS NULL OR LENGTH(currency) = 3)
);

-- =============================================================================
-- INDEXES for parcel
-- =============================================================================
CREATE INDEX idx_parcel_agency_id ON parcel (agency_id);
CREATE INDEX idx_parcel_shipment_id ON parcel (shipment_id);
CREATE INDEX idx_parcel_tracking_number ON parcel (tracking_number);
CREATE INDEX idx_parcel_status ON parcel (status);
CREATE INDEX idx_parcel_agency_status ON parcel (agency_id, status);
CREATE INDEX idx_parcel_current_location ON parcel (current_location_id);
CREATE INDEX idx_parcel_last_scan ON parcel (last_scan_at);
CREATE INDEX idx_parcel_delivered_at ON parcel (delivered_at);
CREATE INDEX idx_parcel_deleted_at ON parcel (deleted_at);

-- Composite index for active parcels
CREATE INDEX idx_parcel_active ON parcel (agency_id, status, deleted_at) WHERE deleted_at IS NULL;

-- Composite index for tracking
CREATE INDEX idx_parcel_tracking_scan ON parcel (tracking_number, last_scan_at);

-- =============================================================================
-- COMMENTS for parcel
-- =============================================================================
COMMENT
ON TABLE parcel IS 'Individual parcels (operational tracking units)';
COMMENT
ON COLUMN parcel.id IS 'Unique identifier for the parcel';
COMMENT
ON COLUMN parcel.agency_id IS 'Agency owning this parcel (tenant isolation)';
COMMENT
ON COLUMN parcel.shipment_id IS 'Shipment to which this parcel belongs';
COMMENT
ON COLUMN parcel.tracking_number IS 'Unique tracking number';
COMMENT
ON COLUMN parcel.status IS 'Parcel status (lifecycle)';
COMMENT
ON COLUMN parcel.weight IS 'Parcel weight (kg)';
COMMENT
ON COLUMN parcel.length IS 'Parcel length (cm)';
COMMENT
ON COLUMN parcel.width IS 'Parcel width (cm)';
COMMENT
ON COLUMN parcel.height IS 'Parcel height (cm)';
COMMENT
ON COLUMN parcel.description IS 'Content description';
COMMENT
ON COLUMN parcel.declared_value IS 'Declared value';
COMMENT
ON COLUMN parcel.current_location_id IS 'Current location (agency_location)';
COMMENT
ON COLUMN parcel.last_scan_at IS 'Last scan timestamp';
COMMENT
ON COLUMN parcel.created_at IS 'Creation timestamp';
COMMENT
ON COLUMN parcel.updated_at IS 'Last update timestamp';
COMMENT
ON COLUMN parcel.delivered_at IS 'Delivery timestamp';
COMMENT
ON COLUMN parcel.deleted_at IS 'Soft delete timestamp';

-- =============================================================================
-- TRIGGER: updated_at auto-update for parcel
-- =============================================================================
CREATE
OR REPLACE FUNCTION update_parcel_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_parcel_updated_at
    BEFORE UPDATE
    ON parcel
    FOR EACH ROW
    EXECUTE FUNCTION update_parcel_updated_at();
