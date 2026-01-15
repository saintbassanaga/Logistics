#!/bin/bash
# ============================================
# Keycloak Local Development Setup Script
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERTS_DIR="${SCRIPT_DIR}/certs"

echo "============================================"
echo "Logistics Platform - Keycloak Setup"
echo "============================================"
echo ""

# Create certs directory
mkdir -p "${CERTS_DIR}"

# Generate self-signed certificate for local development
if [ ! -f "${CERTS_DIR}/server.crt.pem" ]; then
    echo "Generating self-signed certificates for local development..."

    openssl req -x509 \
        -newkey rsa:4096 \
        -keyout "${CERTS_DIR}/server.key.pem" \
        -out "${CERTS_DIR}/server.crt.pem" \
        -days 365 \
        -nodes \
        -subj "/CN=localhost/O=Logistics Platform/C=US" \
        -addext "subjectAltName=DNS:localhost,DNS:keycloak,IP:127.0.0.1"

    chmod 644 "${CERTS_DIR}/server.crt.pem"
    chmod 600 "${CERTS_DIR}/server.key.pem"

    echo "✅ Certificates generated in ${CERTS_DIR}"
else
    echo "✅ Certificates already exist"
fi

# Create .env file if not exists
if [ ! -f "${SCRIPT_DIR}/.env" ]; then
    echo "Creating .env file from template..."

    cat > "${SCRIPT_DIR}/.env" << 'EOF'
# Keycloak Development Environment
# ================================

# Database
KC_DB_PASSWORD=keycloak_dev_password

# Admin
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# Hostname (local development)
KC_HOSTNAME=localhost
KC_HOSTNAME_PORT=8443

# Frontend URL
FRONTEND_BASE_URL=http://localhost:3000

# Service Account (development only - change in production!)
SERVICE_ACCOUNT_SECRET=dev-service-secret-12345

# Platform Admin Initial Password
PLATFORM_ADMIN_PASSWORD=PlatformAdmin123!

# SMTP (uses mailhog in dev)
SMTP_HOST=mailhog
SMTP_PORT=1025
SMTP_USER=
SMTP_PASSWORD=
SMTP_FROM=noreply@localhost
SMTP_REPLY_TO=support@localhost
SMTP_ENVELOPE_FROM=noreply@localhost
EOF

    echo "✅ .env file created"
else
    echo "✅ .env file already exists"
fi

echo ""
echo "============================================"
echo "Setup Complete!"
echo "============================================"
echo ""
echo "To start Keycloak:"
echo "  docker compose -f docker-compose.keycloak.yml --profile dev up -d"
echo ""
echo "Access points:"
echo "  - Keycloak Admin:  https://localhost:8443/admin"
echo "  - Keycloak Health: http://localhost:9000/health"
echo "  - MailHog UI:      http://localhost:8025"
echo ""
echo "Default credentials:"
echo "  - Keycloak Admin:  admin / admin"
echo "  - Platform Admin:  platform-admin / PlatformAdmin123! (will prompt to change)"
echo ""
echo "⚠️  Remember: Accept self-signed certificate warning in browser"
echo ""