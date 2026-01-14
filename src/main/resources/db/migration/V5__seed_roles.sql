-- Seed Data: Initial Roles
-- These are the standard roles used across the platform

-- =============================================================================
-- PLATFORM-SCOPED ROLES
-- =============================================================================

INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'PLATFORM_ADMIN', 'Platform Administrator', 'Full platform administration rights',
        'PLATFORM', true);

-- =============================================================================
-- AGENCY-SCOPED ROLES
-- =============================================================================

-- Agency Management
INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'AGENCY_ADMIN', 'Agency Administrator', 'Full agency administration rights', 'AGENCY',
        true),
       (uuid_generate_v4(), 'AGENCY_MANAGER', 'Agency Manager', 'Agency operations management', 'AGENCY', true);

-- Shipment Management
INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'SHIPMENT_MANAGER', 'Shipment Manager', 'Create and manage shipments', 'AGENCY', true),
       (uuid_generate_v4(), 'SHIPMENT_CLERK', 'Shipment Clerk', 'Register shipments', 'AGENCY', true);

-- Parcel Management
INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'PARCEL_MANAGER', 'Parcel Manager', 'Create and manage parcels', 'AGENCY', true),
       (uuid_generate_v4(), 'PARCEL_CLERK', 'Parcel Clerk', 'Register parcels', 'AGENCY', true);

-- Operations
INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'SORTING_OPERATOR', 'Sorting Operator', 'Parcel sorting and scanning', 'AGENCY', true),
       (uuid_generate_v4(), 'DELIVERY_DRIVER', 'Delivery Driver', 'Parcel delivery and status updates', 'AGENCY', true),
       (uuid_generate_v4(), 'WAREHOUSE_OPERATOR', 'Warehouse Operator', 'Warehouse operations', 'AGENCY', true);

-- Location Management
INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'LOCATION_MANAGER', 'Location Manager', 'Manage agency locations', 'AGENCY', true);

-- Customer Service
INSERT INTO role (id, code, name, description, scope, active)
VALUES (uuid_generate_v4(), 'CUSTOMER_SERVICE', 'Customer Service', 'Customer support and tracking', 'AGENCY', true);

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT
ON TABLE role IS 'Seeded with standard platform roles';