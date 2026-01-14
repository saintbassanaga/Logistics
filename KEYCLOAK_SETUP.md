# Keycloak Setup Guide - Logistics Platform

This guide explains how to configure Keycloak with MFA/TOTP for the Logistics platform.

## Architecture Overview

- **Keycloak**: OAuth2/OIDC Authorization Server (port 8080)
- **Spring Boot**: Resource Server (port 8081)
- **Database**: PostgreSQL (port 5434)
- **Realm**: logistics

## Quick Start

### 1. Start Services

```bash
# Start PostgreSQL and Keycloak
docker-compose up -d

# Wait for Keycloak to be ready (check logs)
docker logs -f logistics-keycloak

# Start Spring Boot application
./gradlew bootRun
```

### 2. Access Keycloak Admin Console

- URL: http://localhost:8080/admin
- Username: `admin`
- Password: `admin123`

## Keycloak Configuration

### Initial Setup

The realm is auto-imported from `keycloak-realm-export.json`. If not, create manually:

#### Step 1: Create Realm

1. Click "Create Realm"
2. Name: `logistics`
3. Enabled: ON
4. Save

#### Step 2: Configure Realm Settings

Navigate to **Realm Settings**:

**Login Tab:**

- User registration: OFF (managed via API)
- Email as username: ON
- Login with email: ON
- Remember me: ON
- Verify email: ON
- Reset password: ON

**Tokens Tab:**

- Access Token Lifespan: 5 minutes (300s)
- SSO Session Idle: 30 minutes
- SSO Session Max: 10 hours

**Security Defenses Tab:**

- Brute Force Detection: ON
- Permanent Lockout: OFF
- Max Login Failures: 5
- Wait Increment: 60 seconds
- Max Wait: 900 seconds

#### Step 3: Configure OTP Policy

Navigate to **Authentication > OTP Policy**:

- OTP Type: **Time-Based (TOTP)**
- OTP Hash Algorithm: HmacSHA1
- Number of Digits: 6
- Look Ahead Window: 1
- OTP Token Period: 30 seconds
- Reusable: OFF

Supported authenticator apps:

- Google Authenticator
- Microsoft Authenticator
- Authy
- FreeOTP

#### Step 4: Create Authentication Flow with TOTP

Navigate to **Authentication > Flows**:

1. Click "Create flow"
2. Name: `browser-with-totp`
3. Add executions:
    - Cookie: ALTERNATIVE
    - Username Password Form: REQUIRED
    - OTP Form: **REQUIRED**

4. Go to **Bindings** tab
5. Set Browser Flow: `browser-with-totp`

This enforces TOTP for all logins.

#### Step 5: Create Clients

**Backend Client (Resource Server):**

```json
{
  "clientId": "logistics-backend",
  "enabled": true,
  "bearerOnly": true,
  "publicClient": false,
  "standardFlowEnabled": false,
  "directAccessGrantsEnabled": false
}
```

**Frontend Client (Public):**

```json
{
  "clientId": "logistics-frontend",
  "enabled": true,
  "publicClient": true,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "redirectUris": [
    "http://localhost:3000/*"
  ],
  "webOrigins": [
    "http://localhost:3000"
  ],
  "pkce.code.challenge.method": "S256"
}
```

#### Step 6: Create Roles

Navigate to **Realm Roles** and create:

**Platform Roles:**

- `PLATFORM_ADMIN`

**Agency Roles:**

- `AGENCY_ADMIN`
- `AGENCY_MANAGER`
- `SHIPMENT_MANAGER`
- `PARCEL_MANAGER`
- `DELIVERY_DRIVER`
- `SORTING_OPERATOR`
- `CUSTOMER_SERVICE`

#### Step 7: Configure Protocol Mappers

Navigate to **Client Scopes > roles > Mappers**:

**Add Mapper: actor_type**

- Name: actor_type
- Mapper Type: User Attribute
- User Attribute: actor_type
- Token Claim Name: actor_type
- Claim JSON Type: String
- Add to ID token: ON
- Add to access token: ON
- Add to userinfo: ON

**Add Mapper: agency_id**

- Name: agency_id
- Mapper Type: User Attribute
- User Attribute: agency_id
- Token Claim Name: agency_id
- Claim JSON Type: String
- Add to ID token: ON
- Add to access token: ON
- Add to userinfo: ON

**Add Mapper: roles**

- Name: roles
- Mapper Type: User Realm Role
- Token Claim Name: roles
- Claim JSON Type: String
- Multivalued: ON
- Add to ID token: ON
- Add to access token: ON
- Add to userinfo: ON

## User Management

### Creating Users via Keycloak UI

1. Navigate to **Users > Add User**
2. Fill required fields:
    - Username: user email
    - Email: user email
    - Email Verified: ON
    - Enabled: ON

3. Click **Save**

4. Go to **Credentials** tab:
    - Set Password
    - Temporary: OFF (or ON for first-time)

5. Go to **Attributes** tab:
    - Add `actor_type`: AGENCY_EMPLOYEE | CUSTOMER | PLATFORM_ADMIN
    - Add `agency_id`: UUID (if AGENCY_EMPLOYEE)

6. Go to **Role Mappings** tab:
    - Assign appropriate realm roles

### Creating Users via API

Users should be created via the Auth API endpoints (to be implemented) which will:

1. Create user in `platform_user` table
2. Sync with Keycloak via Admin API
3. Set appropriate attributes
4. Assign roles

## MFA/TOTP Setup for Users

### Admin-Enforced MFA

1. Navigate to **Authentication > Required Actions**
2. Enable "Configure OTP"
3. Set as Default Action: ON

All new users will be required to configure TOTP on first login.

### User TOTP Setup Flow

When a user logs in for the first time (or when TOTP is required):

1. User enters username/password
2. Keycloak redirects to TOTP setup page
3. User scans QR code with authenticator app:
    - Google Authenticator
    - Microsoft Authenticator
    - Authy
    - FreeOTP

4. User enters 6-digit code from app
5. TOTP is registered
6. All future logins require TOTP code

### Testing TOTP

1. Create a test user
2. Login to frontend
3. Scan QR code with authenticator app
4. Enter OTP code
5. Verify successful authentication

## JWT Structure

After successful authentication, Keycloak issues a JWT with this structure:

```json
{
  "exp": 1735123456,
  "iat": 1735120456,
  "jti": "uuid",
  "iss": "http://localhost:8080/realms/logistics",
  "sub": "user-uuid",
  "typ": "Bearer",
  "azp": "logistics-frontend",
  "actor_type": "AGENCY_EMPLOYEE",
  "agency_id": "agency-uuid",
  "roles": [
    "AGENCY_ADMIN",
    "SHIPMENT_MANAGER"
  ],
  "email": "user@example.com",
  "email_verified": true
}
```

## Security Best Practices

### Password Policy

Configured in **Authentication > Policies > Password Policy**:

- Minimum Length: 8
- Uppercase: 1
- Lowercase: 1
- Digits: 1
- Special Characters: 1
- Not Username

### Session Management

- Access Token Lifespan: 5 minutes
- Refresh Token: 30 minutes idle timeout
- SSO Session: 10 hours max
- Remember Me: 1 week

### Brute Force Protection

- Max Failures: 5
- Wait Increment: 60s
- Max Wait: 15 minutes
- Quick Login Check: 1 second

## Integration with Spring Boot

The Spring Boot app is configured as an OAuth2 Resource Server:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/logistics
          jwk-set-uri: http://localhost:8080/realms/logistics/protocol/openid-connect/certs
```

JWT validation happens automatically. The application:

1. Validates JWT signature using JWK
2. Validates issuer, expiration, audience
3. Extracts custom claims (actor_type, agency_id, roles)
4. Never touches the database for authentication

## Troubleshooting

### Keycloak won't start

```bash
# Check logs
docker logs logistics-keycloak

# Ensure PostgreSQL is running
docker ps | grep postgres

# Recreate Keycloak
docker-compose down
docker volume rm logistics_keycloak_data
docker-compose up -d keycloak
```

### JWT Validation Fails

1. Check issuer-uri matches realm
2. Verify JWK endpoint is accessible
3. Check clock synchronization
4. Verify token hasn't expired
5. Check Spring Security logs

### TOTP Setup Issues

1. Ensure time is synchronized on both devices
2. Check authenticator app supports TOTP
3. Verify OTP Policy settings in Keycloak
4. Try resetting OTP for the user

## Production Considerations

For production deployment:

1. **Use HTTPS** everywhere
2. **Change admin password** immediately
3. **Configure proper hostname** in KC_HOSTNAME
4. **Use external database** (not dev mode)
5. **Enable rate limiting** at reverse proxy
6. **Configure proper CORS** origins
7. **Set up backup** for Keycloak database
8. **Monitor failed login** attempts
9. **Implement JWT blacklist** for logout
10. **Configure email** server for notifications

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth 2.0 RFC](https://tools.ietf.org/html/rfc6749)
- [TOTP RFC 6238](https://tools.ietf.org/html/rfc6238)
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)