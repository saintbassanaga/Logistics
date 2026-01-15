# Logistics Platform API Documentation

> A complete guide to the Logistics API for frontend developers building the Angular web application.

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication & Security](#authentication--security)
3. [User Types & Permissions](#user-types--permissions)
4. [Agency Module](#agency-module)
5. [Auth Module](#auth-module)
6. [Parcel Module](#parcel-module)
7. [Workflows & User Journeys](#workflows--user-journeys)
8. [Enums Reference](#enums-reference)
9. [Error Handling](#error-handling)
10. [Angular Integration Tips](#angular-integration-tips)

---

## Overview

The Logistics Platform is a multi-tenant SaaS application for managing shipping agencies, shipments, and parcel tracking. The API follows REST principles with JWT-based authentication.

### Base URL
```
http://localhost:8080
```

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Agency** | A shipping company/business that uses the platform |
| **Location** | Physical places belonging to an agency (warehouses, branches, pickup points) |
| **Shipment** | A group of parcels being sent together |
| **Parcel** | Individual package with tracking number |
| **User** | Anyone using the platform (customers, employees, admins) |

---

## Authentication & Security

### JWT Token Structure

All authenticated requests require a Bearer token in the header:

```http
Authorization: Bearer <your-jwt-token>
```

### JWT Claims (What's inside the token)

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | UUID | User ID |
| `actor_type` | String | Type of user (CUSTOMER, AGENCY_EMPLOYEE, PLATFORM_ADMIN) |
| `agency_id` | UUID | Agency the user belongs to (null for customers/admins) |
| `roles` | Array | List of role codes (e.g., ["AGENCY_ADMIN", "SHIPMENT_MANAGER"]) |

### Token Storage (Angular)

```typescript
// Store in localStorage or sessionStorage
localStorage.setItem('access_token', token);

// Create an HTTP interceptor to add token to all requests
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('access_token');
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return next.handle(req);
  }
}
```

---

## User Types & Permissions

### Actor Types (Who can use the app)

| Actor Type | Description | Has Agency? | What they can do |
|------------|-------------|-------------|------------------|
| `CUSTOMER` | Regular users who want to ship packages | No | Register, create shipments, track parcels |
| `AGENCY_EMPLOYEE` | Staff working for a shipping agency | Yes | Manage shipments, parcels, validate orders |
| `PLATFORM_ADMIN` | Super admin managing the entire platform | No | Manage all agencies, users, roles |

### Common Roles

| Role Code | Who has it | Permissions |
|-----------|------------|-------------|
| `AGENCY_ADMIN` | Agency owner/manager | Full control over agency, users, locations |
| `AGENCY_MANAGER` | Senior staff | Validate shipments, manage operations |
| `SHIPMENT_MANAGER` | Shipment staff | Create, confirm, validate shipments |
| `SHIPMENT_CLERK` | Junior staff | Validate customer shipments |
| `PARCEL_MANAGER` | Parcel staff | Update parcel status, manage parcels |
| `DELIVERY_DRIVER` | Drivers | Mark parcels as delivered/failed |
| `LOCATION_MANAGER` | Location staff | Close/reopen locations |

---

## Agency Module

### Register a New Agency

When a customer wants to create their own shipping business.

```http
POST /agencies/register
```

**Who can call:** Customers only (`CUSTOMER`)

**Request Body:**
```json
{
  "name": "Express Logistics",
  "legalName": "Express Logistics SARL",
  "email": "contact@express-logistics.com",
  "phone": "+237 699 123 456",
  "website": "https://express-logistics.com",
  "addressLine1": "123 Business Street",
  "addressLine2": "Building A",
  "city": "Douala",
  "stateRegion": "Littoral",
  "postalCode": "12345",
  "country": "CM",
  "defaultCurrency": "XAF",
  "timezone": "Africa/Douala",
  "locale": "fr_CM",
  "taxId": "TAX123456",
  "vatNumber": "VAT789012",
  "transportLicenseNumber": "TL-2024-001",
  "maxShipmentsPerMonth": 1000,
  "maxUsers": 50,
  "subscriptionTier": "PROFESSIONAL"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "code": "EXP-2024-001",
  "name": "Express Logistics",
  "active": true,
  "suspended": false,
  "locationCount": 0,
  "activeLocationCount": 0,
  "createdAt": "2024-01-15T10:30:00Z",
  "locations": []
}
```

**What happens:** The customer becomes `AGENCY_ADMIN` of the new agency.

---

### List All Agencies

```http
GET /agencies
```

**Who can call:** Anyone (public endpoint)

**Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "EXP-2024-001",
    "name": "Express Logistics",
    "city": "Douala",
    "country": "CM",
    "active": true
  }
]
```

---

### Get Agency Details

```http
GET /agencies/{id}
```

**Who can call:** Any authenticated user

**Response:** Full agency details with locations

---

### Suspend/Unsuspend Agency

```http
POST /agencies/{agencyId}/suspend?reason=Payment%20overdue
POST /agencies/{agencyId}/unsuspend
```

**Who can call:** Platform Admins only (`PLATFORM_ADMIN`)

---

### Add Location to Agency

```http
POST /agencies/{agencyId}/locations
```

**Who can call:** Agency Admins (`AGENCY_ADMIN`)

**Request Body:**
```json
{
  "code": "DLA-WH-001",
  "name": "Douala Main Warehouse",
  "locationType": "WAREHOUSE",
  "addressLine1": "Zone Industrielle Bassa",
  "city": "Douala",
  "stateRegion": "Littoral",
  "postalCode": "12345",
  "country": "CM",
  "latitude": 4.0511,
  "longitude": 9.7679,
  "phone": "+237 699 111 222",
  "contactPersonName": "Jean Dupont",
  "contactPersonPhone": "+237 699 333 444",
  "openingHours": "Mon-Sat 8:00-18:00",
  "isPickupPoint": true,
  "isDeliveryPoint": true,
  "isWarehouse": true,
  "maxDailyParcels": 500,
  "storageCapacityM3": 1000.0
}
```

**Location Types:**
- `HEADQUARTERS` - Main office
- `BRANCH` - Branch office
- `WAREHOUSE` - Storage facility
- `PICKUP_POINT` - Customer pickup location
- `SORTING_CENTER` - Parcel sorting facility

---

### List Agency Locations

```http
GET /agencies/{agencyId}/locations
```

---

### Close/Reopen Location

```http
POST /agencies/locations/{id}/close?reason=Renovation
POST /agencies/locations/{id}/reopen
```

**Who can call:** Location Managers (`LOCATION_MANAGER`)

---

## Auth Module

### Self-Registration (New Customer)

```http
POST /auth/register
```

**Who can call:** Anyone (public endpoint)

**Request Body:**
```json
{
  "username": "johndoe",
  "email": "john.doe@email.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+237 699 555 666"
}
```

**Password Requirements:**
- 8-100 characters
- At least 1 lowercase letter
- At least 1 uppercase letter
- At least 1 digit
- At least 1 special character (@$!%*?&)

**Response (201 Created):**
```json
{
  "id": "user-uuid",
  "username": "johndoe",
  "email": "john.doe@email.com",
  "firstName": "John",
  "lastName": "Doe",
  "actorType": "CUSTOMER",
  "message": "Registration successful. Please check your email to verify your account."
}
```

---

### Check Email/Username Availability

```http
GET /auth/check-email?email=john@email.com
GET /auth/check-username?username=johndoe
```

**Response:**
```json
{
  "taken": false,
  "available": true
}
```

**Use case:** Real-time validation in registration form.

---

### Get Current User Profile

```http
GET /auth/profile
GET /users/me
```

**Who can call:** Any authenticated user

**Response:**
```json
{
  "id": "user-uuid",
  "username": "johndoe",
  "email": "john.doe@email.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+237 699 555 666",
  "actorType": "CUSTOMER",
  "agencyId": null,
  "roles": ["USER"],
  "active": true,
  "emailVerified": true,
  "createdAt": "2024-01-15T10:00:00Z",
  "lastLoginAt": "2024-01-15T14:30:00Z"
}
```

---

### Update Profile

```http
PUT /auth/profile
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phone": "+237 699 777 888"
}
```

---

### Change Password

```http
PUT /auth/password
```

**Request Body:**
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewSecurePass456!"
}
```

---

### Create User (Admin)

```http
POST /users
```

**Who can call:**
- `PLATFORM_ADMIN` - Can create any user type
- `AGENCY_ADMIN` - Can only create `AGENCY_EMPLOYEE` for their agency

**Request Body:**
```json
{
  "email": "employee@agency.com",
  "firstName": "Marie",
  "lastName": "Dupont",
  "phone": "+237 699 888 999",
  "actorType": "AGENCY_EMPLOYEE",
  "agencyId": "agency-uuid",
  "jobTitle": "Shipment Manager",
  "department": "Operations"
}
```

---

### Assign/Remove Role

```http
POST /users/{id}/assign-role
```

**Request Body:**
```json
{
  "roleId": "role-uuid"
}
```

```http
DELETE /users/{id}/remove-role/{roleId}
```

---

### List Agency Users

```http
GET /users/agency/{agencyId}
```

---

### Deactivate/Activate User

```http
POST /users/{id}/deactivate
POST /users/{id}/activate
```

---

### Role Management (Platform Admin)

```http
POST /roles                    # Create role
GET /roles                     # List all roles
GET /roles/active              # List active roles
GET /roles/{id}                # Get role by ID
GET /roles/code/{code}         # Get role by code
PUT /roles/{id}                # Update role
POST /roles/{id}/deactivate    # Deactivate role
POST /roles/{id}/activate      # Activate role
```

**Create Role Request:**
```json
{
  "code": "INVENTORY_MANAGER",
  "name": "Inventory Manager",
  "description": "Manages warehouse inventory",
  "scope": "AGENCY"
}
```

---

## Parcel Module

### The Shipment Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│                    CUSTOMER CREATES SHIPMENT                      │
│                              │                                    │
│                              ▼                                    │
│                   ┌─────────────────────┐                        │
│                   │ PENDING_VALIDATION  │ ◄── Waiting for review │
│                   └─────────────────────┘                        │
│                         │         │                              │
│                 validate│         │reject                        │
│                         ▼         ▼                              │
│              ┌─────────┐   ┌──────────┐                         │
│              │  OPEN   │   │ REJECTED │                         │
│              └─────────┘   └──────────┘                         │
│                   │                                              │
│           confirm │ (after adding parcels)                       │
│                   ▼                                              │
│              ┌───────────┐                                       │
│              │ CONFIRMED │ ◄── Ready for processing              │
│              └───────────┘                                       │
└──────────────────────────────────────────────────────────────────┘
```

---

### Customer Creates Shipment

```http
POST /customer/shipments
```

**Who can call:** Customers only (`CUSTOMER`)

**Request Body:**
```json
{
  "agencyId": "agency-uuid",
  "pickupLocationId": "location-uuid",
  "senderName": "John Doe",
  "senderPhone": "+237 699 111 111",
  "senderEmail": "john@email.com",
  "senderAddressLine1": "123 Sender Street",
  "senderCity": "Douala",
  "senderPostalCode": "12345",
  "senderCountry": "CM",
  "receiverName": "Jane Smith",
  "receiverPhone": "+237 699 222 222",
  "receiverEmail": "jane@email.com",
  "receiverAddressLine1": "456 Receiver Avenue",
  "receiverCity": "Yaoundé",
  "receiverPostalCode": "54321",
  "receiverCountry": "CM",
  "totalWeight": 5.5,
  "declaredValue": 50000,
  "currency": "XAF",
  "notes": "Fragile items - handle with care"
}
```

**Response (201 Created):**
```json
{
  "id": "shipment-uuid",
  "shipmentNumber": "SHP-2024-00001",
  "status": "PENDING_VALIDATION",
  "customerId": "customer-uuid",
  "pickupLocationId": "location-uuid",
  "senderName": "John Doe",
  "receiverName": "Jane Smith",
  "totalWeight": 5.5,
  "declaredValue": 50000,
  "currency": "XAF",
  "parcelCount": 0,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

---

### Customer Lists Their Shipments

```http
GET /customer/shipments
GET /customer/shipments?status=PENDING_VALIDATION
```

---

### Customer Updates/Cancels Shipment

Only works when status is `PENDING_VALIDATION`.

```http
PUT /customer/shipments/{id}
DELETE /customer/shipments/{id}
```

---

### Agency Employee Validates Shipments

**List pending shipments:**
```http
GET /shipments/validation/pending
GET /shipments/validation/pending?locationId={locationId}
GET /shipments/validation/pending/count
```

**Validate shipment (approve):**
```http
POST /shipments/validation/{id}/validate
```

**Request Body (optional):**
```json
{
  "notes": "Validated - customer verified"
}
```

**Reject shipment:**
```http
POST /shipments/validation/{id}/reject
```

**Request Body:**
```json
{
  "reason": "Invalid receiver address - please provide complete address"
}
```

---

### Agency Creates Shipment Directly

```http
POST /shipments
```

**Who can call:** Agency Employees (`AGENCY_EMPLOYEE`)

This creates a shipment with `OPEN` status directly (skips validation).

---

### List Agency Shipments

```http
GET /shipments              # All shipments
GET /shipments/open         # Only OPEN shipments
GET /shipments/{id}         # Single shipment with parcels
```

---

### Confirm Shipment

After adding parcels, confirm the shipment:

```http
POST /shipments/{id}/confirm
```

**Who can call:** Shipment Managers (`SHIPMENT_MANAGER`)

---

### Add Parcel to Shipment

```http
POST /shipments/{shipmentId}/parcels
```

**Who can call:** Agency Employees (`AGENCY_EMPLOYEE`)

**Request Body:**
```json
{
  "weight": 2.5,
  "length": 30,
  "width": 20,
  "height": 15,
  "description": "Electronics - Laptop",
  "declaredValue": 350000,
  "currency": "XAF",
  "specificReceiverName": "Jane Smith",
  "specificReceiverPhone": "+237 699 222 222",
  "notes": "Fragile - handle with care"
}
```

**Response:**
```json
{
  "id": "parcel-uuid",
  "trackingNumber": "TRK-2024-ABC123",
  "status": "REGISTERED",
  "weight": 2.5,
  "description": "Electronics - Laptop",
  "createdAt": "2024-01-15T11:00:00Z"
}
```

---

### The Parcel Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                      PARCEL STATUS FLOW                          │
│                                                                  │
│   ┌────────────┐                                                │
│   │ REGISTERED │ ◄── Parcel created, ready for pickup           │
│   └────────────┘                                                │
│         │                                                        │
│         ▼                                                        │
│   ┌────────────┐                                                │
│   │ IN_TRANSIT │ ◄── On the way                                 │
│   └────────────┘                                                │
│         │                                                        │
│         ▼                                                        │
│   ┌────────────┐                                                │
│   │ IN_SORTING │ ◄── At sorting center                          │
│   └────────────┘                                                │
│         │                                                        │
│         ▼                                                        │
│   ┌──────────────────┐                                          │
│   │ OUT_FOR_DELIVERY │ ◄── With delivery driver                 │
│   └──────────────────┘                                          │
│         │                                                        │
│    ┌────┴────┐                                                  │
│    ▼         ▼                                                  │
│ ┌───────────┐ ┌────────┐                                       │
│ │ DELIVERED │ │ FAILED │                                       │
│ └───────────┘ └────────┘                                       │
│                   │                                              │
│                   ▼                                              │
│              ┌──────────┐                                       │
│              │ RETURNED │                                       │
│              └──────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

### Track Parcel

```http
GET /parcels/{id}
GET /parcels/tracking/{trackingNumber}
```

**Response:**
```json
{
  "id": "parcel-uuid",
  "trackingNumber": "TRK-2024-ABC123",
  "status": "IN_TRANSIT",
  "weight": 2.5,
  "description": "Electronics - Laptop",
  "currentLocationId": "location-uuid",
  "lastScanAt": "2024-01-15T14:30:00Z",
  "createdAt": "2024-01-15T11:00:00Z"
}
```

---

### Update Parcel Status

```http
PUT /parcels/{id}/status
```

**Who can call:** Parcel Managers (`PARCEL_MANAGER`)

**Request Body:**
```json
{
  "newStatus": "IN_TRANSIT",
  "locationId": "current-location-uuid"
}
```

---

### Mark Parcel as Delivered

```http
POST /parcels/{id}/deliver?receivedBy=Jane%20Smith
```

**Who can call:** Delivery Drivers (`DELIVERY_DRIVER`)

---

### Mark Parcel as Failed

```http
POST /parcels/{id}/fail?reason=Recipient%20not%20available
```

**Who can call:** Delivery Drivers (`DELIVERY_DRIVER`)

---

### List Parcels

```http
GET /shipments/{shipmentId}/parcels    # Parcels in a shipment
GET /parcels/active                     # Active parcels for agency
```

---

## Workflows & User Journeys

### 1. New Customer Registration Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. User visits registration page                                 │
│                                                                  │
│ 2. Angular app checks availability in real-time:                 │
│    GET /auth/check-email?email=...                              │
│    GET /auth/check-username?username=...                        │
│                                                                  │
│ 3. User submits form:                                           │
│    POST /auth/register                                          │
│                                                                  │
│ 4. Redirect to login page with success message                  │
│                                                                  │
│ 5. User logs in (via Keycloak/OAuth)                            │
│                                                                  │
│ 6. Store JWT token                                              │
│                                                                  │
│ 7. Fetch profile: GET /auth/profile                             │
│                                                                  │
│ 8. Redirect based on actorType:                                 │
│    - CUSTOMER → Customer Dashboard                              │
│    - AGENCY_EMPLOYEE → Agency Dashboard                         │
│    - PLATFORM_ADMIN → Admin Dashboard                           │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Customer Creates Shipment Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Customer selects agency: GET /agencies                        │
│                                                                  │
│ 2. Customer views agency locations:                              │
│    GET /agencies/{agencyId}/locations                           │
│                                                                  │
│ 3. Customer fills shipment form and submits:                     │
│    POST /customer/shipments                                     │
│                                                                  │
│ 4. Customer sees confirmation with shipmentNumber                │
│                                                                  │
│ 5. Customer can track status: GET /customer/shipments/{id}       │
│                                                                  │
│ 6. If rejected, customer can update and resubmit:               │
│    PUT /customer/shipments/{id}                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3. Agency Employee Validation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Employee dashboard shows pending count:                       │
│    GET /shipments/validation/pending/count                      │
│                                                                  │
│ 2. Employee clicks to see pending list:                          │
│    GET /shipments/validation/pending                            │
│                                                                  │
│ 3. Employee reviews shipment details:                            │
│    GET /shipments/validation/{id}                               │
│                                                                  │
│ 4. Employee validates or rejects:                                │
│    POST /shipments/validation/{id}/validate                     │
│    POST /shipments/validation/{id}/reject                       │
│                                                                  │
│ 5. If validated, shipment moves to OPEN status                   │
│    Employee can now add parcels                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 4. Agency Registration Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Customer (who wants to start a business) logs in              │
│                                                                  │
│ 2. Customer fills agency registration form:                      │
│    POST /agencies/register                                      │
│                                                                  │
│ 3. System creates agency with unique code                        │
│    Customer becomes AGENCY_ADMIN                                │
│                                                                  │
│ 4. Customer's actorType changes to AGENCY_EMPLOYEE               │
│    (Refresh profile: GET /auth/profile)                         │
│                                                                  │
│ 5. New AGENCY_ADMIN can now:                                     │
│    - Add locations: POST /agencies/{id}/locations               │
│    - Create employees: POST /users                              │
│    - Assign roles: POST /users/{id}/assign-role                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5. Parcel Tracking Flow (Driver)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Driver logs in and sees assigned parcels:                     │
│    GET /parcels/active                                          │
│                                                                  │
│ 2. Driver picks up parcel, updates status:                       │
│    PUT /parcels/{id}/status                                     │
│    { "newStatus": "OUT_FOR_DELIVERY" }                          │
│                                                                  │
│ 3. Driver arrives at destination:                                │
│                                                                  │
│    SUCCESS → POST /parcels/{id}/deliver?receivedBy=John         │
│                                                                  │
│    FAILURE → POST /parcels/{id}/fail?reason=Not%20home          │
│                                                                  │
│ 4. Parcel status updated, customer notified                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Enums Reference

### Actor Types
```typescript
enum ActorType {
  CUSTOMER = 'CUSTOMER',           // Regular users
  AGENCY_EMPLOYEE = 'AGENCY_EMPLOYEE', // Agency staff
  PLATFORM_ADMIN = 'PLATFORM_ADMIN'    // Super admins
}
```

### Subscription Tiers
```typescript
enum SubscriptionTier {
  BASIC = 'BASIC',
  PROFESSIONAL = 'PROFESSIONAL',
  ENTERPRISE = 'ENTERPRISE',
  CUSTOM = 'CUSTOM'
}
```

### Location Types
```typescript
enum LocationType {
  HEADQUARTERS = 'HEADQUARTERS',
  BRANCH = 'BRANCH',
  WAREHOUSE = 'WAREHOUSE',
  PICKUP_POINT = 'PICKUP_POINT',
  SORTING_CENTER = 'SORTING_CENTER'
}
```

### Role Scopes
```typescript
enum RoleScope {
  PLATFORM = 'PLATFORM',  // Platform-wide roles
  AGENCY = 'AGENCY',      // Agency-specific roles
  CUSTOMER = 'CUSTOMER'   // Customer roles
}
```

### Shipment Status
```typescript
enum ShipmentStatus {
  PENDING_VALIDATION = 'PENDING_VALIDATION', // Waiting for agency review
  OPEN = 'OPEN',                             // Validated, can add parcels
  CONFIRMED = 'CONFIRMED',                   // Ready for processing
  REJECTED = 'REJECTED'                      // Rejected by agency
}
```

### Parcel Status
```typescript
enum ParcelStatus {
  REGISTERED = 'REGISTERED',           // Created, not yet picked up
  IN_TRANSIT = 'IN_TRANSIT',           // On the way
  IN_SORTING = 'IN_SORTING',           // At sorting center
  OUT_FOR_DELIVERY = 'OUT_FOR_DELIVERY', // With delivery driver
  DELIVERED = 'DELIVERED',             // Successfully delivered
  FAILED = 'FAILED',                   // Delivery failed
  RETURNED = 'RETURNED'                // Returned to sender
}
```

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | When it happens |
|------|---------|-----------------|
| `200` | OK | Successful GET, PUT |
| `201` | Created | Successful POST (resource created) |
| `204` | No Content | Successful action, no response body |
| `400` | Bad Request | Invalid input, validation error |
| `401` | Unauthorized | Missing or invalid token |
| `403` | Forbidden | Valid token but insufficient permissions |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | Duplicate email/username, business rule violation |
| `500` | Server Error | Something went wrong on server |

### Angular Error Handling

```typescript
// error.interceptor.ts
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        switch (error.status) {
          case 401:
            // Token expired, redirect to login
            localStorage.removeItem('access_token');
            this.router.navigate(['/login']);
            break;
          case 403:
            this.snackBar.open('You don\'t have permission for this action', 'Close');
            break;
          case 404:
            this.snackBar.open('Resource not found', 'Close');
            break;
          case 409:
            this.snackBar.open(error.error.message || 'Conflict error', 'Close');
            break;
          default:
            this.snackBar.open('An error occurred', 'Close');
        }
        return throwError(() => error);
      })
    );
  }
}
```

---

## Angular Integration Tips

### 1. Project Structure

```
src/app/
├── core/
│   ├── auth/
│   │   ├── auth.service.ts
│   │   ├── auth.guard.ts
│   │   └── auth.interceptor.ts
│   ├── models/
│   │   ├── user.model.ts
│   │   ├── agency.model.ts
│   │   ├── shipment.model.ts
│   │   └── parcel.model.ts
│   └── services/
│       ├── agency.service.ts
│       ├── shipment.service.ts
│       └── parcel.service.ts
├── features/
│   ├── customer/
│   │   ├── dashboard/
│   │   ├── shipments/
│   │   └── tracking/
│   ├── agency/
│   │   ├── dashboard/
│   │   ├── shipments/
│   │   ├── parcels/
│   │   ├── locations/
│   │   └── users/
│   └── admin/
│       ├── agencies/
│       ├── users/
│       └── roles/
└── shared/
    ├── components/
    └── pipes/
```

### 2. TypeScript Models

```typescript
// models/user.model.ts
export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  actorType: ActorType;
  agencyId?: string;
  jobTitle?: string;
  department?: string;
  roles: string[];
  active: boolean;
  emailVerified: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

// models/shipment.model.ts
export interface Shipment {
  id: string;
  shipmentNumber: string;
  status: ShipmentStatus;
  customerId?: string;
  agencyId: string;
  pickupLocationId?: string;
  senderName: string;
  senderPhone?: string;
  senderEmail?: string;
  senderAddressLine1: string;
  senderCity: string;
  senderPostalCode: string;
  senderCountry: string;
  receiverName: string;
  receiverPhone?: string;
  receiverEmail?: string;
  receiverAddressLine1: string;
  receiverCity: string;
  receiverPostalCode: string;
  receiverCountry: string;
  totalWeight: number;
  declaredValue?: number;
  currency?: string;
  notes?: string;
  parcelCount: number;
  parcels?: Parcel[];
  createdAt: string;
  confirmedAt?: string;
}

// models/parcel.model.ts
export interface Parcel {
  id: string;
  trackingNumber: string;
  status: ParcelStatus;
  shipmentId: string;
  agencyId: string;
  weight: number;
  length?: number;
  width?: number;
  height?: number;
  description: string;
  declaredValue?: number;
  currency?: string;
  currentLocationId?: string;
  lastScanAt?: string;
  deliveredAt?: string;
  createdAt: string;
}
```

### 3. Services Example

```typescript
// services/shipment.service.ts
@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // Customer endpoints
  createCustomerShipment(request: CustomerShipmentRequest): Observable<Shipment> {
    return this.http.post<Shipment>(`${this.baseUrl}/customer/shipments`, request);
  }

  getCustomerShipments(status?: ShipmentStatus): Observable<Shipment[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Shipment[]>(`${this.baseUrl}/customer/shipments`, { params });
  }

  // Agency endpoints
  getAgencyShipments(): Observable<Shipment[]> {
    return this.http.get<Shipment[]>(`${this.baseUrl}/shipments`);
  }

  getShipment(id: string): Observable<Shipment> {
    return this.http.get<Shipment>(`${this.baseUrl}/shipments/${id}`);
  }

  confirmShipment(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/shipments/${id}/confirm`, {});
  }

  // Validation endpoints
  getPendingShipments(locationId?: string): Observable<Shipment[]> {
    let params = new HttpParams();
    if (locationId) {
      params = params.set('locationId', locationId);
    }
    return this.http.get<Shipment[]>(`${this.baseUrl}/shipments/validation/pending`, { params });
  }

  validateShipment(id: string, notes?: string): Observable<Shipment> {
    return this.http.post<Shipment>(
      `${this.baseUrl}/shipments/validation/${id}/validate`,
      notes ? { notes } : {}
    );
  }

  rejectShipment(id: string, reason: string): Observable<Shipment> {
    return this.http.post<Shipment>(
      `${this.baseUrl}/shipments/validation/${id}/reject`,
      { reason }
    );
  }
}
```

### 4. Route Guards

```typescript
// auth.guard.ts
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const user = this.authService.currentUser;

    if (!user) {
      this.router.navigate(['/login']);
      return false;
    }

    // Check required actor type
    const requiredActorType = route.data['actorType'] as ActorType;
    if (requiredActorType && user.actorType !== requiredActorType) {
      this.router.navigate(['/unauthorized']);
      return false;
    }

    // Check required roles
    const requiredRoles = route.data['roles'] as string[];
    if (requiredRoles && !requiredRoles.some(role => user.roles.includes(role))) {
      this.router.navigate(['/unauthorized']);
      return false;
    }

    return true;
  }
}

// Usage in routes
const routes: Routes = [
  {
    path: 'customer',
    canActivate: [AuthGuard],
    data: { actorType: 'CUSTOMER' },
    children: [...]
  },
  {
    path: 'agency',
    canActivate: [AuthGuard],
    data: { actorType: 'AGENCY_EMPLOYEE' },
    children: [
      {
        path: 'validation',
        data: { roles: ['SHIPMENT_MANAGER', 'SHIPMENT_CLERK', 'AGENCY_ADMIN'] },
        children: [...]
      }
    ]
  }
];
```

### 5. Dashboard Ideas by User Type

**Customer Dashboard:**
- My Shipments (with status filter)
- Create New Shipment button
- Track Parcel (by tracking number)
- Profile settings

**Agency Employee Dashboard:**
- Pending Validations count (badge)
- Open Shipments list
- Active Parcels list
- Quick actions (create shipment, add parcel)

**Agency Admin Dashboard:**
- All employee dashboards features +
- Manage Locations
- Manage Users
- Agency Settings

**Platform Admin Dashboard:**
- All Agencies list
- Suspend/Unsuspend agencies
- Manage platform roles
- Create platform users

---

## Quick Reference

### Most Used Endpoints

| Action | Method | Endpoint |
|--------|--------|----------|
| Register | POST | `/auth/register` |
| Get profile | GET | `/auth/profile` |
| Create customer shipment | POST | `/customer/shipments` |
| List my shipments | GET | `/customer/shipments` |
| Track parcel | GET | `/parcels/tracking/{number}` |
| List pending validations | GET | `/shipments/validation/pending` |
| Validate shipment | POST | `/shipments/validation/{id}/validate` |
| Add parcel | POST | `/shipments/{id}/parcels` |
| Mark delivered | POST | `/parcels/{id}/deliver` |
| List agencies | GET | `/agencies` |
| Register agency | POST | `/agencies/register` |

---

**Happy coding!** If you have questions about specific endpoints or need clarification, refer to this documentation or test the API using tools like Postman or curl.