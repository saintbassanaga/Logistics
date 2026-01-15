# Keycloak Integration Guide

> How the Logistics Platform integrates with Keycloak for authentication and how the two databases work together.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Database Schema Separation](#database-schema-separation)
3. [How Authentication Works](#how-authentication-works)
4. [JWT Token Structure](#jwt-token-structure)
5. [User Synchronization Strategy](#user-synchronization-strategy)
6. [Keycloak Configuration](#keycloak-configuration)
7. [Security Flow Step by Step](#security-flow-step-by-step)
8. [Multi-Tenancy Implementation](#multi-tenancy-implementation)
9. [Angular Integration](#angular-integration)
10. [Development Setup](#development-setup)

---

## Architecture Overview

The Logistics Platform uses a **dual-database architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ARCHITECTURE                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐         ┌─────────────┐         ┌─────────────┐         │
│   │   Angular   │         │  Keycloak   │         │  Spring     │         │
│   │   Frontend  │         │   Server    │         │  Boot API   │         │
│   └──────┬──────┘         └──────┬──────┘         └──────┬──────┘         │
│          │                       │                       │                 │
│          │ 1. Login              │                       │                 │
│          │──────────────────────►│                       │                 │
│          │                       │                       │                 │
│          │ 2. JWT Token          │                       │                 │
│          │◄──────────────────────│                       │                 │
│          │                       │                       │                 │
│          │ 3. API Request + JWT                          │                 │
│          │──────────────────────────────────────────────►│                 │
│          │                       │                       │                 │
│          │                       │ 4. Validate JWT       │                 │
│          │                       │◄──────────────────────│                 │
│          │                       │                       │                 │
│          │ 5. Response                                   │                 │
│          │◄──────────────────────────────────────────────│                 │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                           POSTGRESQL DATABASE                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────┐    ┌─────────────────────────┐               │
│   │    keycloak schema      │    │     public schema       │               │
│   │    ─────────────────    │    │     ─────────────────   │               │
│   │                         │    │                         │               │
│   │  • user_entity          │    │  • platform_user        │               │
│   │  • credential           │    │  • role                 │               │
│   │  • user_attribute       │    │  • user_role            │               │
│   │  • user_role_mapping    │    │  • agency               │               │
│   │  • keycloak_role        │    │  • agency_location      │               │
│   │  • realm                │    │  • shipment             │               │
│   │  • client               │    │  • parcel               │               │
│   │  • ...                  │    │  • ...                  │               │
│   │                         │    │                         │               │
│   │  Managed by: Keycloak   │    │  Managed by: Flyway     │               │
│   └─────────────────────────┘    └─────────────────────────┘               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Principles

| Concern | Responsibility | Storage |
|---------|---------------|---------|
| **Authentication** | Keycloak | `keycloak` schema |
| **Authorization** | Spring Boot App | `public` schema |
| **User Identity** | Both (synchronized) | Both schemas |
| **Business Data** | Spring Boot App | `public` schema |

---

## Database Schema Separation

### Same PostgreSQL Instance, Different Schemas

Both Keycloak and the application share the **same PostgreSQL database** but use **different schemas**:

```yaml
# docker-compose.yml

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: logistics
      POSTGRES_USER: transport_user
      POSTGRES_PASSWORD: transport_password
    ports:
      - "5434:5432"

  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/logistics
      KC_DB_USERNAME: transport_user
      KC_DB_PASSWORD: transport_password
      KC_DB_SCHEMA: keycloak  # <-- Keycloak uses its own schema
```

### Schema Responsibilities

#### `keycloak` Schema (Managed by Keycloak)
- User credentials (passwords, OTP secrets)
- Authentication sessions
- Realm configuration
- Client configurations
- User attributes (actor_type, agency_id)
- Role mappings (for JWT)

#### `public` Schema (Managed by Flyway)
- Business entities (Agency, Shipment, Parcel)
- User profile data (`platform_user`)
- Application roles (`role`)
- Role assignments (`user_role`)

### Why Two Schemas?

```
┌─────────────────────────────────────────────────────────────────┐
│                    SEPARATION OF CONCERNS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  KEYCLOAK SCHEMA                    PUBLIC SCHEMA                │
│  ══════════════                     ═════════════                │
│                                                                  │
│  ✓ Password hashing                 ✓ Business logic             │
│  ✓ Session management               ✓ Agency data                │
│  ✓ OAuth2/OIDC compliance           ✓ Shipment tracking          │
│  ✓ MFA/2FA                          ✓ User profiles              │
│  ✓ Token generation                 ✓ Role management            │
│  ✓ Brute force protection           ✓ Multi-tenancy              │
│                                                                  │
│  You NEVER touch this directly      You manage this via Flyway   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## How Authentication Works

### The Complete Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         AUTHENTICATION FLOW                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  STEP 1: User Login                                                          │
│  ─────────────────                                                           │
│                                                                              │
│  User ──────────────────────────────────────────────────────► Keycloak       │
│       │  POST /realms/logistics/protocol/openid-connect/token               │
│       │  {                                                                   │
│       │    "grant_type": "password",                                        │
│       │    "client_id": "logistics-frontend",                               │
│       │    "username": "john@email.com",                                    │
│       │    "password": "SecurePass123!"                                     │
│       │  }                                                                   │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  STEP 2: Keycloak Validates & Returns JWT                                    │
│  ───────────────────────────────────────                                     │
│                                                                              │
│  Keycloak:                                                                   │
│    1. Checks credentials in keycloak.user_entity                            │
│    2. Verifies password hash                                                │
│    3. Reads user attributes (actor_type, agency_id)                         │
│    4. Reads role mappings                                                   │
│    5. Builds JWT with custom claims                                         │
│    6. Signs JWT with private key                                            │
│                                                                              │
│  Keycloak ──────────────────────────────────────────────────► User          │
│           │  {                                                               │
│           │    "access_token": "eyJhbGciOiJSUzI1NiI...",                    │
│           │    "refresh_token": "eyJhbGciOiJIUzI1NiI...",                   │
│           │    "token_type": "Bearer",                                      │
│           │    "expires_in": 300                                            │
│           │  }                                                               │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  STEP 3: API Request with JWT                                                │
│  ──────────────────────────────                                              │
│                                                                              │
│  User ──────────────────────────────────────────────────────► Spring Boot   │
│       │  GET /shipments                                                     │
│       │  Authorization: Bearer eyJhbGciOiJSUzI1NiI...                       │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  STEP 4: Spring Boot Validates JWT                                           │
│  ─────────────────────────────────                                           │
│                                                                              │
│  Spring Boot:                                                                │
│    1. Extracts JWT from Authorization header                                │
│    2. Fetches Keycloak's public key (JWK)                                   │
│    3. Verifies JWT signature                                                │
│    4. Checks expiration                                                     │
│    5. Extracts claims (sub, actor_type, agency_id, roles)                   │
│    6. Creates SecurityContext                                               │
│    7. Sets TenantContext (agency_id)                                        │
│    8. Processes request                                                     │
│                                                                              │
│  Spring Boot ───────────────────────────────────────────────► User          │
│              │  { "shipments": [...] }                                       │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## JWT Token Structure

### Custom Claims in JWT

Keycloak is configured to include custom claims in the JWT access token:

```json
{
  "exp": 1705320000,
  "iat": 1705319700,
  "iss": "http://localhost:8080/realms/logistics",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "typ": "Bearer",
  "azp": "logistics-frontend",

  "actor_type": "AGENCY_EMPLOYEE",
  "agency_id": "660e8400-e29b-41d4-a716-446655440001",
  "roles": ["AGENCY_ADMIN", "SHIPMENT_MANAGER"]
}
```

### Claim Descriptions

| Claim | Source | Description |
|-------|--------|-------------|
| `sub` | Keycloak User ID | Unique user identifier (UUID) |
| `actor_type` | User Attribute | Type of user (CUSTOMER, AGENCY_EMPLOYEE, PLATFORM_ADMIN) |
| `agency_id` | User Attribute | Agency UUID (only for AGENCY_EMPLOYEE) |
| `roles` | Realm Roles | List of role codes assigned to user |
| `iss` | Keycloak Config | Token issuer URL |
| `exp` | Keycloak Config | Token expiration timestamp |

### How Claims are Mapped

Keycloak uses **Protocol Mappers** to include custom claims:

```json
// keycloak-realm-export.json

"protocolMappers": [
  {
    "name": "actor_type",
    "protocol": "openid-connect",
    "protocolMapper": "oidc-usermodel-attribute-mapper",
    "config": {
      "user.attribute": "actor_type",      // Read from user attributes
      "claim.name": "actor_type",          // Include as JWT claim
      "access.token.claim": "true",
      "id.token.claim": "true"
    }
  },
  {
    "name": "agency_id",
    "protocol": "openid-connect",
    "protocolMapper": "oidc-usermodel-attribute-mapper",
    "config": {
      "user.attribute": "agency_id",
      "claim.name": "agency_id",
      "access.token.claim": "true"
    }
  },
  {
    "name": "roles",
    "protocol": "openid-connect",
    "protocolMapper": "oidc-usermodel-realm-role-mapper",
    "config": {
      "claim.name": "roles",
      "multivalued": "true",
      "access.token.claim": "true"
    }
  }
]
```

---

## User Synchronization Strategy

### The Dual-Storage Approach

Users exist in **both** Keycloak and the application database:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         USER DATA DISTRIBUTION                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  KEYCLOAK (keycloak schema)              APP (public schema)                │
│  ══════════════════════════              ══════════════════                 │
│                                                                             │
│  keycloak.user_entity                    public.platform_user               │
│  ┌─────────────────────────┐             ┌─────────────────────────┐       │
│  │ id (Keycloak UUID)      │             │ id (App UUID)           │       │
│  │ username                │             │ email                   │       │
│  │ email                   │ ◄─────────► │ first_name              │       │
│  │ email_verified          │  SYNCED     │ last_name               │       │
│  │ enabled                 │             │ phone                   │       │
│  │ created_timestamp       │             │ actor_type              │       │
│  └─────────────────────────┘             │ agency_id               │       │
│                                          │ external_auth_id ───────┼──┐    │
│  keycloak.credential                     │ job_title               │  │    │
│  ┌─────────────────────────┐             │ department              │  │    │
│  │ password_hash           │             │ active                  │  │    │
│  │ salt                    │             │ email_verified          │  │    │
│  │ type                    │             │ created_at              │  │    │
│  └─────────────────────────┘             │ last_login_at           │  │    │
│                                          └─────────────────────────┘  │    │
│  keycloak.user_attribute                                              │    │
│  ┌─────────────────────────┐                                          │    │
│  │ actor_type = "..."      │ ◄────────────────────────────────────────┘    │
│  │ agency_id = "..."       │  Links Keycloak user to App user              │
│  └─────────────────────────┘                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why Duplicate User Data?

| Data | Stored In | Reason |
|------|-----------|--------|
| Password | Keycloak only | Security - never expose password handling |
| actor_type | Both | Keycloak needs it for JWT, App needs it for business logic |
| agency_id | Both | Keycloak needs it for JWT, App needs it for queries |
| Roles | Both | Keycloak for JWT, App for fine-grained permissions |
| Profile (name, phone) | App only | Business data, not needed for authentication |
| Job title, Department | App only | Business data specific to the app |

### Synchronization Approaches

#### Option 1: Keycloak as Source of Truth (Current)

```
┌──────────────────────────────────────────────────────────────────┐
│                    REGISTRATION FLOW                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. User registers via POST /auth/register                       │
│                 │                                                │
│                 ▼                                                │
│  2. App creates user in Keycloak via Admin API                   │
│     - Creates user_entity                                        │
│     - Sets password                                              │
│     - Sets user attributes (actor_type, agency_id)               │
│     - Assigns realm roles                                        │
│                 │                                                │
│                 ▼                                                │
│  3. App creates platform_user in public schema                   │
│     - Stores external_auth_id (Keycloak user ID)                 │
│     - Stores profile data                                        │
│     - Links to agency if AGENCY_EMPLOYEE                         │
│                 │                                                │
│                 ▼                                                │
│  4. User can now login via Keycloak                              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

#### Option 2: Event-Based Sync (Advanced)

```
┌──────────────────────────────────────────────────────────────────┐
│                    EVENT-BASED SYNC                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Keycloak                         App                            │
│     │                               │                            │
│     │  User Created Event           │                            │
│     │──────────────────────────────►│                            │
│     │                               │ Create platform_user       │
│     │                               │                            │
│     │  User Updated Event           │                            │
│     │──────────────────────────────►│                            │
│     │                               │ Update platform_user       │
│     │                               │                            │
│     │  Role Assigned Event          │                            │
│     │──────────────────────────────►│                            │
│     │                               │ Update user_role           │
│     │                               │                            │
│                                                                  │
│  (Requires Keycloak Event Listener SPI)                          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Keycloak Configuration

### Realm Configuration

```json
{
  "realm": "logistics",
  "enabled": true,
  "displayName": "Logistics Platform",

  "registrationAllowed": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "verifyEmail": true,

  "passwordPolicy": "length(8) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1)",

  "bruteForceProtected": true,
  "failureFactor": 5,
  "maxFailureWaitSeconds": 900,

  "accessTokenLifespan": 300,
  "ssoSessionIdleTimeout": 1800,
  "ssoSessionMaxLifespan": 36000
}
```

### Clients Configuration

#### Backend Client (Resource Server)

```json
{
  "clientId": "logistics-backend",
  "name": "Logistics Backend Service",
  "enabled": true,
  "publicClient": false,
  "bearerOnly": true,
  "standardFlowEnabled": false
}
```

#### Frontend Client (Angular App)

```json
{
  "clientId": "logistics-frontend",
  "name": "Logistics Frontend Application",
  "enabled": true,
  "publicClient": true,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "redirectUris": [
    "http://localhost:4200/*"
  ],
  "webOrigins": [
    "http://localhost:4200"
  ],
  "attributes": {
    "pkce.code.challenge.method": "S256"
  }
}
```

### Realm Roles

```json
{
  "roles": {
    "realm": [
      { "name": "PLATFORM_ADMIN", "description": "Platform Administrator" },
      { "name": "AGENCY_ADMIN", "description": "Agency Administrator" },
      { "name": "AGENCY_MANAGER", "description": "Agency Manager" },
      { "name": "SHIPMENT_MANAGER", "description": "Shipment Manager" },
      { "name": "PARCEL_MANAGER", "description": "Parcel Manager" },
      { "name": "DELIVERY_DRIVER", "description": "Delivery Driver" }
    ]
  }
}
```

---

## Security Flow Step by Step

### Spring Boot Security Chain

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SPRING SECURITY FILTER CHAIN                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  HTTP Request                                                               │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  BearerTokenAuthenticationFilter                                     │   │
│  │  ─────────────────────────────────                                   │   │
│  │  1. Extract JWT from Authorization header                            │   │
│  │  2. Validate JWT signature using Keycloak's JWK                      │   │
│  │  3. Check token expiration                                           │   │
│  │  4. Create Authentication object with JWT                            │   │
│  │  5. Store in SecurityContextHolder                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  TenantContextFilter (Custom)                                        │   │
│  │  ────────────────────────────                                        │   │
│  │  1. Get JWT from SecurityContextHolder                               │   │
│  │  2. Extract agency_id claim                                          │   │
│  │  3. Store in TenantContext (ThreadLocal)                             │   │
│  │  4. Continue filter chain                                            │   │
│  │  5. Clear TenantContext in finally block                             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Controller Method                                                   │   │
│  │  ─────────────────                                                   │   │
│  │  @RequireRole("SHIPMENT_MANAGER")  ◄── Checked by AOP aspect         │   │
│  │  @RequireActor(AGENCY_EMPLOYEE)    ◄── Checked by AOP aspect         │   │
│  │                                                                      │   │
│  │  SecurityContext context = securityContextService.get...();          │   │
│  │  // context.userId(), context.actorType(), context.agencyId()        │   │
│  │  // context.roles(), context.hasRole("...")                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### SecurityContextService

```java
@Service
public class SecurityContextService {

    public SecurityContext getCurrentSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();

        return new SecurityContext(
            extractUserId(jwt),        // from 'sub' claim
            extractActorType(jwt),     // from 'actor_type' claim
            extractAgencyId(jwt),      // from 'agency_id' claim
            extractRoles(jwt)          // from 'roles' claim
        );
    }
}
```

### SecurityContext Record

```java
public record SecurityContext(
    UUID userId,
    ActorType actorType,
    UUID agencyId,
    Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAgencyEmployee() {
        return actorType == ActorType.AGENCY_EMPLOYEE;
    }

    public boolean isPlatformAdmin() {
        return actorType == ActorType.PLATFORM_ADMIN;
    }

    public boolean belongsToAgency(UUID agencyId) {
        return isAgencyEmployee() && this.agencyId.equals(agencyId);
    }
}
```

---

## Multi-Tenancy Implementation

### How Tenant Isolation Works

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MULTI-TENANCY FLOW                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  JWT Token                                                                  │
│  ┌────────────────────────────────────────┐                                │
│  │ {                                      │                                │
│  │   "sub": "user-uuid",                  │                                │
│  │   "actor_type": "AGENCY_EMPLOYEE",     │                                │
│  │   "agency_id": "agency-123",  ◄────────┼── Tenant identifier            │
│  │   "roles": ["SHIPMENT_MANAGER"]        │                                │
│  │ }                                      │                                │
│  └────────────────────────────────────────┘                                │
│                     │                                                       │
│                     ▼                                                       │
│  TenantContextFilter                                                        │
│  ┌────────────────────────────────────────┐                                │
│  │ TenantContext.setCurrentAgencyId(      │                                │
│  │     "agency-123"                       │                                │
│  │ );                                     │                                │
│  └────────────────────────────────────────┘                                │
│                     │                                                       │
│                     ▼                                                       │
│  Database Query                                                             │
│  ┌────────────────────────────────────────┐                                │
│  │ SELECT * FROM shipment                 │                                │
│  │ WHERE agency_id = 'agency-123'  ◄──────┼── Automatic tenant filtering   │
│  └────────────────────────────────────────┘                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### TenantContext (ThreadLocal)

```java
public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_AGENCY_ID = new ThreadLocal<>();

    public static void setCurrentAgencyId(UUID agencyId) {
        CURRENT_AGENCY_ID.set(agencyId);
    }

    public static UUID getCurrentAgencyId() {
        return CURRENT_AGENCY_ID.get();
    }

    public static void clear() {
        CURRENT_AGENCY_ID.remove();
    }
}
```

### TenantAware Interface

All tenant-scoped entities implement this interface:

```java
public interface TenantAware {
    UUID getAgencyId();
    void setAgencyId(UUID agencyId);
}

// Example: Shipment entity
@Entity
public class Shipment implements TenantAware {

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    // ... other fields
}
```

---

## Angular Integration

### Keycloak Angular Setup

```bash
npm install keycloak-angular keycloak-js
```

### Keycloak Configuration

```typescript
// src/app/core/auth/keycloak.config.ts

import { KeycloakOptions } from 'keycloak-angular';

export const keycloakConfig: KeycloakOptions = {
  config: {
    url: 'http://localhost:8080',
    realm: 'logistics',
    clientId: 'logistics-frontend'
  },
  initOptions: {
    onLoad: 'check-sso',
    silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html',
    checkLoginIframe: false,
    pkceMethod: 'S256'
  },
  enableBearerInterceptor: true,
  bearerPrefix: 'Bearer',
  bearerExcludedUrls: [
    '/assets',
    '/auth/register',
    '/auth/check-email',
    '/auth/check-username'
  ]
};
```

### App Initialization

```typescript
// src/app/app.config.ts

import { APP_INITIALIZER, ApplicationConfig } from '@angular/core';
import { KeycloakService, KeycloakAngularModule } from 'keycloak-angular';
import { keycloakConfig } from './core/auth/keycloak.config';

function initializeKeycloak(keycloak: KeycloakService) {
  return () => keycloak.init(keycloakConfig);
}

export const appConfig: ApplicationConfig = {
  providers: [
    KeycloakAngularModule,
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService]
    }
  ]
};
```

### Auth Service

```typescript
// src/app/core/auth/auth.service.ts

import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { KeycloakProfile } from 'keycloak-js';

export interface UserClaims {
  sub: string;
  actor_type: 'CUSTOMER' | 'AGENCY_EMPLOYEE' | 'PLATFORM_ADMIN';
  agency_id?: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  constructor(private keycloak: KeycloakService) {}

  // Check if user is logged in
  isLoggedIn(): boolean {
    return this.keycloak.isLoggedIn();
  }

  // Get user profile
  async getUserProfile(): Promise<KeycloakProfile> {
    return this.keycloak.loadUserProfile();
  }

  // Get custom claims from token
  getUserClaims(): UserClaims {
    const token = this.keycloak.getKeycloakInstance().tokenParsed;
    return {
      sub: token?.sub || '',
      actor_type: token?.['actor_type'] || 'CUSTOMER',
      agency_id: token?.['agency_id'],
      roles: token?.['roles'] || []
    };
  }

  // Check if user has specific role
  hasRole(role: string): boolean {
    const claims = this.getUserClaims();
    return claims.roles.includes(role);
  }

  // Check actor type
  isAgencyEmployee(): boolean {
    return this.getUserClaims().actor_type === 'AGENCY_EMPLOYEE';
  }

  isCustomer(): boolean {
    return this.getUserClaims().actor_type === 'CUSTOMER';
  }

  isPlatformAdmin(): boolean {
    return this.getUserClaims().actor_type === 'PLATFORM_ADMIN';
  }

  // Get agency ID
  getAgencyId(): string | undefined {
    return this.getUserClaims().agency_id;
  }

  // Login
  login(): void {
    this.keycloak.login();
  }

  // Logout
  logout(): void {
    this.keycloak.logout(window.location.origin);
  }

  // Get access token
  async getToken(): Promise<string> {
    return this.keycloak.getToken();
  }
}
```

### Route Guards

```typescript
// src/app/core/auth/auth.guard.ts

import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { KeycloakAuthGuard, KeycloakService } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class AuthGuard extends KeycloakAuthGuard {

  constructor(
    protected override router: Router,
    protected override keycloakAngular: KeycloakService
  ) {
    super(router, keycloakAngular);
  }

  async isAccessAllowed(route: ActivatedRouteSnapshot): Promise<boolean> {
    // Check if logged in
    if (!this.authenticated) {
      await this.keycloakAngular.login();
      return false;
    }

    // Check required actor type
    const requiredActorType = route.data['actorType'];
    if (requiredActorType) {
      const token = this.keycloakAngular.getKeycloakInstance().tokenParsed;
      const actorType = token?.['actor_type'];

      if (actorType !== requiredActorType) {
        this.router.navigate(['/unauthorized']);
        return false;
      }
    }

    // Check required roles
    const requiredRoles = route.data['roles'] as string[];
    if (requiredRoles && requiredRoles.length > 0) {
      const token = this.keycloakAngular.getKeycloakInstance().tokenParsed;
      const userRoles = token?.['roles'] as string[] || [];

      const hasRole = requiredRoles.some(role => userRoles.includes(role));
      if (!hasRole) {
        this.router.navigate(['/unauthorized']);
        return false;
      }
    }

    return true;
  }
}
```

### Silent Check SSO

Create file: `src/assets/silent-check-sso.html`

```html
<!doctype html>
<html>
<body>
  <script>
    parent.postMessage(location.href, location.origin);
  </script>
</body>
</html>
```

---

## Development Setup

### Starting the Environment

```bash
# 1. Start PostgreSQL and Keycloak
docker-compose up -d

# 2. Wait for Keycloak to be ready (about 60 seconds)
docker-compose logs -f keycloak

# 3. Start Spring Boot application
./gradlew bootRun

# 4. Start Angular application
cd frontend && ng serve
```

### Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| Keycloak Admin Console | http://localhost:8080 | admin / admin123 |
| Keycloak Realm | http://localhost:8080/realms/logistics | - |
| Spring Boot API | http://localhost:8081 | - |
| Swagger UI | http://localhost:8081/swagger-ui.html | - |
| Angular App | http://localhost:4200 | - |

### Creating Test Users in Keycloak

1. Go to http://localhost:8080
2. Login with admin / admin123
3. Select "logistics" realm
4. Go to Users > Add user
5. Fill in:
   - Username: `test@agency.com`
   - Email: `test@agency.com`
   - Email Verified: ON
   - Enabled: ON
6. Go to Credentials tab:
   - Set password
   - Temporary: OFF
7. Go to Attributes tab:
   - Add `actor_type` = `AGENCY_EMPLOYEE`
   - Add `agency_id` = `<agency-uuid-from-database>`
8. Go to Role Mappings:
   - Assign roles: AGENCY_ADMIN, SHIPMENT_MANAGER, etc.

### Environment Variables

```bash
# For production deployment
export JWT_ISSUER_URI=https://auth.yourdomain.com/realms/logistics
export JWT_JWK_SET_URI=https://auth.yourdomain.com/realms/logistics/protocol/openid-connect/certs
```

---

## Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         KEY TAKEAWAYS                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. SAME DATABASE, DIFFERENT SCHEMAS                                        │
│     - Keycloak uses 'keycloak' schema                                       │
│     - App uses 'public' schema                                              │
│     - Both share PostgreSQL instance                                        │
│                                                                             │
│  2. KEYCLOAK HANDLES AUTHENTICATION                                         │
│     - Password management                                                   │
│     - Session management                                                    │
│     - Token generation                                                      │
│     - MFA/2FA                                                               │
│                                                                             │
│  3. APP HANDLES AUTHORIZATION                                               │
│     - Business logic                                                        │
│     - Role-based access control (from JWT)                                  │
│     - Multi-tenancy (agency_id from JWT)                                    │
│     - Data ownership validation                                             │
│                                                                             │
│  4. JWT IS THE BRIDGE                                                       │
│     - Contains user identity (sub)                                          │
│     - Contains actor type (actor_type)                                      │
│     - Contains tenant (agency_id)                                           │
│     - Contains permissions (roles)                                          │
│                                                                             │
│  5. USER DATA IS SYNCHRONIZED                                               │
│     - Keycloak: credentials + attributes for JWT                            │
│     - App: profile data + business relationships                            │
│     - Linked via external_auth_id                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

**Questions?** This documentation covers the core concepts. For specific implementation details, refer to the source code or the API documentation.