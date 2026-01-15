# Logistics Platform - Production Deployment Guide

## Prerequisites

- Docker and Docker Compose installed
- TLS/SSL certificates for HTTPS
- Production database credentials
- SMTP credentials for email notifications
- Domain name configured with DNS

## Security Checklist

Before deploying to production, ensure:

- [ ] All secrets are stored in environment variables, never in code
- [ ] TLS/SSL certificates are properly configured
- [ ] Database passwords are strong and unique
- [ ] Keycloak admin password is strong and unique
- [ ] SMTP credentials are configured
- [ ] API documentation (Swagger) is disabled in production
- [ ] Debug logging is disabled
- [ ] Error messages don't expose sensitive information

## Environment Configuration

### 1. Create Environment File

Copy the example environment file and configure it:

```bash
cp .env.example .env
```

Edit `.env` and set all required variables:

**Critical Variables to Change:**

- `POSTGRES_PASSWORD` - Strong database password
- `KEYCLOAK_ADMIN_PASSWORD` - Strong Keycloak admin password
- `KC_HOSTNAME` - Your production domain (e.g., auth.yourdomain.com)
- `JWT_ISSUER_URI` - Your Keycloak issuer URI
- `JWT_JWK_SET_URI` - Your Keycloak JWK set URI
- `SERVICE_ACCOUNT_SECRET` - Generate with `openssl rand -base64 32`
- `SMTP_PASSWORD` - Your SMTP provider API key

### 2. TLS/SSL Certificates

Place your TLS certificates in the `certs/` directory:

```bash
mkdir -p certs
# Copy your certificates
cp /path/to/your/server.crt.pem certs/
cp /path/to/your/server.key.pem certs/
# Secure the private key
chmod 600 certs/server.key.pem
```

**Certificate Requirements:**

- Valid SSL certificate from a trusted CA (not self-signed for production)
- Private key must be in PEM format
- Certificate chain should be complete

**Generating Self-Signed Certificates (Development/Testing Only):**

```bash
mkdir -p certs
openssl req -x509 -newkey rsa:4096 -nodes \
  -keyout certs/server.key.pem \
  -out certs/server.crt.pem \
  -days 365 \
  -subj "/CN=localhost"
chmod 600 certs/server.key.pem
```

### 3. Production Environment Variables

For production deployment, configure `.env` with:

```bash
# Production mode
KC_HTTP_ENABLED=false
KC_HTTPS_ENABLED=true
KC_HOSTNAME_STRICT=true
KC_HOSTNAME=auth.yourdomain.com
KC_HOSTNAME_PORT=443

# Spring profile
SPRING_PROFILES_ACTIVE=prod

# Disable Swagger in production
API_DOCS_ENABLED=false
SWAGGER_UI_ENABLED=false

# Production logging
KC_LOG_LEVEL=info
```

## Deployment Steps

### 1. Start Infrastructure

```bash
# Start PostgreSQL and Keycloak
docker-compose up -d

# Verify services are running
docker-compose ps

# Check logs
docker-compose logs -f
```

### 2. Verify Keycloak Setup

1. Access Keycloak admin console:
    - Development: `http://localhost:8080/admin`
    - Production: `https://auth.yourdomain.com/admin`

2. Login with credentials from `.env`:
    - Username: Value of `KEYCLOAK_ADMIN`
    - Password: Value of `KEYCLOAK_ADMIN_PASSWORD`

3. Verify realm import:
    - Check that `logistics` realm exists
    - Verify clients are configured
    - Verify roles are created

### 3. Configure SMTP (Email)

In Keycloak Admin Console:

1. Navigate to: Realm Settings → Email
2. Configure SMTP settings from your `.env` file
3. Test email configuration

### 4. Create Initial Admin User

The realm import does not include default users for security. Create the initial platform admin:

1. In Keycloak Admin Console, navigate to: Users → Add User
2. Set username and email
3. Set temporary password in Credentials tab
4. Assign `PLATFORM_ADMIN` role
5. Set user attributes:
    - `actor_type`: `PLATFORM_ADMIN`

### 5. Start Application

```bash
# Build the application
./gradlew clean build

# Run with production profile
export SPRING_PROFILES_ACTIVE=prod
./gradlew bootRun

# Or run the JAR directly
java -jar build/libs/Logistics-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 6. Verify Application

1. Check health endpoint: `http://localhost:8081/actuator/health`
2. Verify JWT validation is working
3. Test authentication flow

## Reverse Proxy Configuration

### Nginx Example

```nginx
server {
    listen 443 ssl http2;
    server_name auth.yourdomain.com;

    ssl_certificate /path/to/ssl/cert.pem;
    ssl_certificate_key /path/to/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://localhost:8443;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
    }
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /path/to/ssl/cert.pem;
    ssl_certificate_key /path/to/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Database Backup

### Backup

```bash
docker exec logistics-db pg_dump -U transport_user logistics > backup-$(date +%Y%m%d-%H%M%S).sql
```

### Restore

```bash
docker exec -i logistics-db psql -U transport_user logistics < backup-20260115-120000.sql
```

## Monitoring

### Health Checks

- Application: `http://localhost:8081/actuator/health`
- Keycloak: `http://localhost:8080/health/ready`
- Database: `docker exec logistics-db pg_isready -U transport_user`

### Logs

```bash
# Application logs
./gradlew bootRun > app.log 2>&1

# Docker logs
docker-compose logs -f keycloak
docker-compose logs -f postgres

# Keycloak logs
docker exec logistics-keycloak cat /opt/keycloak/data/log/keycloak.log
```

### Metrics

Prometheus metrics available at: `http://localhost:8081/actuator/prometheus`

## Security Hardening

### Keycloak

1. **Enforce HTTPS**: Set `KC_HOSTNAME_STRICT=true`
2. **Strong Passwords**: Configure password policy in realm settings
3. **MFA**: Enable TOTP for admin users
4. **Session Timeout**: Configure appropriate session timeouts
5. **Brute Force Protection**: Already enabled in realm configuration
6. **Regular Updates**: Keep Keycloak version up to date

### Application

1. **JWT Validation**: Ensure `JWT_ISSUER_URI` and `JWT_JWK_SET_URI` are correct
2. **CORS**: Configure allowed origins for frontend
3. **Rate Limiting**: Implement rate limiting at reverse proxy level
4. **Input Validation**: Already implemented via Spring Validation
5. **SQL Injection**: Using JPA/Hibernate parameterized queries

### Database

1. **Strong Passwords**: Use strong unique passwords
2. **Network Isolation**: Don't expose PostgreSQL port to public internet
3. **Regular Backups**: Implement automated backup strategy
4. **Encryption**: Enable encryption at rest if required

## Troubleshooting

### Keycloak Won't Start

Check:

- Database connection credentials
- Certificate files exist and have correct permissions
- Hostname configuration matches your domain

### JWT Validation Fails

Check:

- `JWT_ISSUER_URI` matches Keycloak realm issuer
- `JWT_JWK_SET_URI` is accessible from application
- Keycloak is running and accessible

### Email Not Sending

Check:

- SMTP credentials are correct
- SMTP host and port are accessible
- Firewall allows outbound SMTP connections

## Updating

### Application Update

```bash
# Pull latest code
git pull

# Build
./gradlew clean build

# Stop application
# Update and restart
```

### Keycloak Update

```bash
# Stop Keycloak
docker-compose stop keycloak

# Update image version in docker-compose.yml
# Backup database first!

# Start Keycloak
docker-compose up -d keycloak
```

### Database Migration

Flyway handles migrations automatically on application startup. Always backup before upgrading.

## Support

For issues or questions:

- Check application logs: `docker-compose logs app`
- Check Keycloak logs: `docker-compose logs keycloak`
- Review this documentation
- Contact support team